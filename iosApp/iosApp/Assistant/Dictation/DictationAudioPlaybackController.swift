import AVFoundation
import Combine
import Foundation

/// Owns the process-wide playback audio session and player lifecycle.
/// Recording intentionally uses a record-only session, so archive playback
/// must switch categories explicitly before opening the saved CAF.
@MainActor
final class DictationAudioPlaybackController: NSObject, ObservableObject {
    @Published private(set) var playingCaptureId: Int64?

    private var player: AVAudioPlayer?
    private var previousSessionConfiguration: AudioSessionConfiguration?

    func play(path: String, captureId: Int64) throws {
        stop()

        let session = AVAudioSession.sharedInstance()
        previousSessionConfiguration = AudioSessionConfiguration(session: session)
        do {
            try session.setCategory(.playback, mode: .default)
            try session.setActive(true, options: .notifyOthersOnDeactivation)

            let newPlayer = try AVAudioPlayer(contentsOf: URL(fileURLWithPath: path))
            newPlayer.delegate = self
            guard newPlayer.duration > 0, newPlayer.prepareToPlay(), newPlayer.play() else {
                throw DictationAudioPlaybackError.unplayableFile
            }
            player = newPlayer
            playingCaptureId = captureId
        } catch {
            player = nil
            playingCaptureId = nil
            releaseSession()
            throw error
        }
    }

    func stop() {
        player?.stop()
        player = nil
        playingCaptureId = nil
        releaseSession()
    }

    private func releaseSession() {
        let session = AVAudioSession.sharedInstance()
        try? session.setActive(false, options: .notifyOthersOnDeactivation)
        if let previousSessionConfiguration {
            try? session.setCategory(
                previousSessionConfiguration.category,
                mode: previousSessionConfiguration.mode,
                options: previousSessionConfiguration.options
            )
        }
        previousSessionConfiguration = nil
    }
}

private struct AudioSessionConfiguration {
    let category: AVAudioSession.Category
    let mode: AVAudioSession.Mode
    let options: AVAudioSession.CategoryOptions

    init(session: AVAudioSession) {
        category = session.category
        mode = session.mode
        options = session.categoryOptions
    }
}

extension DictationAudioPlaybackController: AVAudioPlayerDelegate {
    nonisolated func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        Task { @MainActor [weak self] in
            self?.stop()
        }
    }

    nonisolated func audioPlayerDecodeErrorDidOccur(_ player: AVAudioPlayer, error: Error?) {
        Task { @MainActor [weak self] in
            self?.stop()
        }
    }
}

private enum DictationAudioPlaybackError: LocalizedError {
    case unplayableFile

    var errorDescription: String? {
        "This recording could not be played."
    }
}

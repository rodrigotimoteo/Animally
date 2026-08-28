import AVFoundation
import Combine
import Foundation

/// Owns the process-wide playback audio session and player lifecycle.
/// Recording intentionally uses a record-only session, so archive playback
/// must switch categories explicitly before opening the saved CAF.
@MainActor
final class DictationAudioPlaybackController: NSObject, ObservableObject {
    @Published private(set) var playingCaptureId: Int64?
    @Published private(set) var currentTime: TimeInterval = 0
    @Published private(set) var duration: TimeInterval = 0
    @Published private(set) var playbackRate: Float = 1.0

    static let supportedRates: [Float] = [1.0, 1.5, 2.0]

    private var player: AVAudioPlayer?
    private var progressTimer: Timer?
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
            guard newPlayer.duration > 0, newPlayer.prepareToPlay() else {
                throw DictationAudioPlaybackError.unplayableFile
            }
            newPlayer.enableRate = true
            newPlayer.rate = playbackRate
            guard newPlayer.play() else {
                throw DictationAudioPlaybackError.unplayableFile
            }
            player = newPlayer
            playingCaptureId = captureId
            currentTime = newPlayer.currentTime
            duration = newPlayer.duration
            startProgressTimer()
        } catch {
            player = nil
            playingCaptureId = nil
            currentTime = 0
            duration = 0
            releaseSession()
            throw error
        }
    }

    /// Seeks the active recording to a clamped position in seconds.
    func seek(to time: TimeInterval) {
        guard let player else { return }
        let clamped = min(max(time, 0), player.duration)
        player.currentTime = clamped
        currentTime = clamped
    }

    /// Changes the active player's playback rate. Unsupported values are ignored.
    func setRate(_ rate: Float) {
        guard Self.supportedRates.contains(rate) else { return }
        playbackRate = rate
        guard let player else { return }
        player.enableRate = true
        player.rate = rate
    }

    /// Synchronizes published progress with AVAudioPlayer for the SwiftUI row.
    func refreshProgress() {
        guard let player else { return }
        currentTime = player.currentTime
        duration = player.duration
        if !player.isPlaying, player.currentTime >= player.duration {
            stop()
        }
    }

    func stop() {
        progressTimer?.invalidate()
        progressTimer = nil
        player?.stop()
        player = nil
        playingCaptureId = nil
        currentTime = 0
        duration = 0
        playbackRate = 1.0
        releaseSession()
    }

    private func startProgressTimer() {
        progressTimer?.invalidate()
        let timer = Timer(timeInterval: 0.1, repeats: true) { [weak self] _ in
            Task { @MainActor [weak self] in
                self?.refreshProgress()
            }
        }
        RunLoop.main.add(timer, forMode: .common)
        progressTimer = timer
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

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

        guard let audioURL = Self.resolvedAudioURL(for: path) else {
            throw DictationAudioPlaybackError.unplayableFile
        }

        let session = AVAudioSession.sharedInstance()
        previousSessionConfiguration = AudioSessionConfiguration(session: session)
        do {
            try session.setCategory(.playback, mode: .default)
            try session.setActive(true, options: .notifyOthersOnDeactivation)

            let newPlayer = try AVAudioPlayer(contentsOf: audioURL)
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

    /// Resolves current and restored dictation paths without allowing reads
    /// outside the app-owned Documents/dictations directory.
    static func resolvedAudioURL(for storedPath: String) -> URL? {
        let trimmedPath = storedPath.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedPath.isEmpty else { return nil }

        let storedURL: URL
        if let parsedURL = URL(string: trimmedPath), parsedURL.scheme != nil {
            guard parsedURL.isFileURL else { return nil }
            storedURL = parsedURL
        } else {
            storedURL = URL(fileURLWithPath: trimmedPath)
        }

        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let dictationsURL = documentsURL.appendingPathComponent("dictations", isDirectory: true)
        if let currentURL = containedExistingFile(storedURL, under: dictationsURL) {
            return currentURL
        }

        guard !storedURL.lastPathComponent.isEmpty else { return nil }
        let fallbackURL = dictationsURL.appendingPathComponent(storedURL.lastPathComponent, isDirectory: false)
        return containedExistingFile(fallbackURL, under: dictationsURL)
    }

    private static func containedExistingFile(_ candidate: URL, under root: URL) -> URL? {
        let canonicalRoot = root.resolvingSymlinksInPath().standardizedFileURL.path
        let canonicalCandidate = candidate.resolvingSymlinksInPath().standardizedFileURL
        let candidatePath = canonicalCandidate.path
        guard candidatePath == canonicalRoot || candidatePath.hasPrefix(canonicalRoot + "/") else {
            return nil
        }
        var isDirectory = ObjCBool(false)
        guard
            FileManager.default.fileExists(atPath: candidatePath, isDirectory: &isDirectory),
            !isDirectory.boolValue
        else {
            return nil
        }
        return canonicalCandidate
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

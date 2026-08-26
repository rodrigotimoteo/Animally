import AVFoundation
import Foundation
import Shared
import SwiftUI

/// Searchable archive of completed dictations. The transcript remains useful
/// even when a recording was not available or has since been removed.
struct DictationArchiveView: View {
    @ObservedObject var viewModel: DictationReviewViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var player: AVAudioPlayer?
    @State private var playingCaptureId: Int64?
    @State private var playbackTask: Task<Void, Never>?
    @State private var playbackError: String?

    private var captures: [DictationCaptureItem] {
        viewModel.state.captures
    }

    private var searchQuery: Binding<String> {
        Binding(
            get: { viewModel.state.captureSearchQuery },
            set: { viewModel.setCaptureSearchQuery($0) }
        )
    }

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.state.isCapturesLoading && captures.isEmpty {
                    ProgressView("Loading dictations…")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if captures.isEmpty {
                    emptyView
                } else {
                    captureList
                }
            }
            .navigationTitle("Dictation history")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                        .accessibilityIdentifier("dictation_archive_done")
                }
            }
            .searchable(text: searchQuery, prompt: "Search transcripts")
            .overlay(alignment: .bottom) {
                if let captureError = viewModel.state.captureError {
                    Label(captureError, systemImage: "exclamationmark.triangle")
                        .font(.footnote)
                        .foregroundStyle(Theme.amber)
                        .padding(10)
                        .frame(maxWidth: .infinity)
                        .background(Theme.surfaceElevated)
                }
            }
            .alert(
                "Recording unavailable",
                isPresented: Binding(
                    get: { playbackError != nil },
                    set: { if !$0 { playbackError = nil } }
                )
            ) {
                Button("OK", role: .cancel) { playbackError = nil }
            } message: {
                Text(playbackError ?? "This dictation only has its transcript available.")
            }
            .onAppear { viewModel.reloadCaptures() }
            .onDisappear { stopPlayback() }
        }
        .accessibilityIdentifier("dictation_archive")
    }

    private var emptyView: some View {
        ContentUnavailableView(
            "No saved dictations",
            systemImage: "waveform.badge.mic",
            description: Text(
                searchQuery.wrappedValue.isEmpty
                    ? "Completed dictations will appear here so you can revisit the original note later."
                    : "No saved dictation matches your search."
            )
        )
    }

    private var captureList: some View {
        List {
            Section {
                ForEach(captures, id: \.id) { capture in
                    captureRow(capture)
                        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                            Button(role: .destructive) {
                                if playingCaptureId == capture.id {
                                    stopPlayback()
                                }
                                viewModel.deleteCapture(id: capture.id)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                }
            } header: {
                Text("Saved notes")
            } footer: {
                Text("Audio stays on this device. Delete an entry when you no longer need the original recording.")
            }
        }
        .listStyle(.insetGrouped)
    }

    private func captureRow(_ capture: DictationCaptureItem) -> some View {
        let hasAudio = hasPlayableAudio(capture)
        return HStack(alignment: .top, spacing: 12) {
            Image(systemName: hasAudio ? "waveform" : "text.quote")
                .font(.title3)
                .foregroundStyle(Theme.forestGreen)
                .frame(width: 34, height: 34)
                .background(Theme.forestGreen.opacity(0.10))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 6) {
                Text(capture.transcript.isEmpty ? "No transcript captured" : capture.transcript)
                    .font(.subheadline)
                    .foregroundStyle(Theme.textPrimary)
                    .lineLimit(4)

                HStack(spacing: 8) {
                    Text(dateText(for: capture))
                    if let duration = capture.durationMillis?.int64Value, duration > 0 {
                        Text("•")
                        Text(durationText(duration))
                    }
                }
                .font(.caption)
                .foregroundStyle(Theme.textSecondary)

                if !hasAudio {
                    Label("Audio not available", systemImage: "waveform.slash")
                        .font(.caption2)
                        .foregroundStyle(Theme.textTertiary)
                }
            }

            Spacer(minLength: 4)

            Button {
                togglePlayback(capture)
            } label: {
                Image(systemName: playingCaptureId == capture.id ? "stop.circle.fill" : "play.circle.fill")
                    .font(.title2)
                    .foregroundStyle(hasAudio ? Theme.forestGreen : Theme.textTertiary)
            }
            .buttonStyle(.plain)
            .disabled(!hasAudio)
            .accessibilityLabel(hasAudio ? "Play recording" : "Recording unavailable")
            .accessibilityIdentifier("dictation_play_\(capture.id)")
        }
        .padding(.vertical, 5)
        .contentShape(Rectangle())
        .accessibilityIdentifier("dictation_capture_\(capture.id)")
    }

    private func dateText(for capture: DictationCaptureItem) -> String {
        Date(timeIntervalSince1970: TimeInterval(capture.capturedAtMillis) / 1000)
            .formatted(date: .abbreviated, time: .shortened)
    }

    private func durationText(_ durationMillis: Int64) -> String {
        let totalSeconds = max(0, durationMillis / 1000)
        return String(format: "%d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    private func hasPlayableAudio(_ capture: DictationCaptureItem) -> Bool {
        guard let path = capture.audioPath, !path.isEmpty else { return false }
        return FileManager.default.fileExists(atPath: path)
    }

    private func togglePlayback(_ capture: DictationCaptureItem) {
        guard let path = capture.audioPath, FileManager.default.fileExists(atPath: path) else {
            playbackError = "The original audio file is no longer available, but the transcript is still saved."
            return
        }

        if playingCaptureId == capture.id {
            stopPlayback()
            return
        }

        stopPlayback()
        do {
            let newPlayer = try AVAudioPlayer(contentsOf: URL(fileURLWithPath: path))
            newPlayer.prepareToPlay()
            guard newPlayer.play() else {
                playbackError = "This recording could not be played."
                return
            }
            player = newPlayer
            playingCaptureId = capture.id
            schedulePlaybackEnd(for: capture)
        } catch {
            playbackError = "This recording could not be opened."
        }
    }

    private func schedulePlaybackEnd(for capture: DictationCaptureItem) {
        playbackTask?.cancel()
        guard let durationMillis = capture.durationMillis?.int64Value, durationMillis > 0 else { return }
        playbackTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: UInt64(durationMillis) * 1_000_000)
            guard !Task.isCancelled, playingCaptureId == capture.id else { return }
            stopPlayback()
        }
    }

    private func stopPlayback() {
        playbackTask?.cancel()
        playbackTask = nil
        player?.stop()
        player = nil
        playingCaptureId = nil
    }
}

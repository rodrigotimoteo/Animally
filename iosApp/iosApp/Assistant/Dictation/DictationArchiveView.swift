import Foundation
import Shared
import SwiftUI

/// Searchable archive of completed dictations. The transcript remains useful
/// even when a recording was not available or has since been removed.
struct DictationArchiveView: View {
    @ObservedObject var viewModel: DictationReviewViewModel
    @EnvironmentObject private var theme: ThemeViewModel
    @Environment(\.dismiss) private var dismiss
    @StateObject private var playback = DictationAudioPlaybackController()
    @State private var playbackError: String?
    @State private var isPlaybackSpeedPickerPresented = false

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
        .tint(theme.accentColor)
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
                        .confirmationSwipeDelete(
                            title: "Saved dictation",
                            message: "The transcript and original audio will be removed from this device."
                        ) {
                            if playback.playingCaptureId == capture.id {
                                stopPlayback()
                            }
                            viewModel.deleteCapture(id: capture.id)
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
        let isPlaying = playback.playingCaptureId == capture.id
        return VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: hasAudio ? "waveform" : "text.quote")
                    .font(.title3)
                    .foregroundStyle(theme.accentColor)
                    .frame(width: 34, height: 34)
                    .background(theme.accentColor.opacity(0.10))
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
                    Image(systemName: isPlaying ? "stop.circle.fill" : "play.circle.fill")
                        .font(.title2)
                        .foregroundStyle(hasAudio ? theme.accentColor : Theme.textTertiary)
                }
                .buttonStyle(.plain)
                .disabled(!hasAudio)
                .accessibilityLabel(
                    hasAudio
                        ? (isPlaying ? "Stop recording playback" : "Play recording")
                        : "Recording unavailable"
                )
                .accessibilityIdentifier("dictation_play_\(capture.id)")
            }

            if isPlaying {
                playbackControls
            }
        }
        .padding(.vertical, 5)
        .contentShape(Rectangle())
        .accessibilityIdentifier("dictation_capture_\(capture.id)")
    }

    private var playbackControls: some View {
        VStack(spacing: 4) {
            Slider(
                value: Binding(
                    get: { playback.currentTime },
                    set: { playback.seek(to: $0) }
                ),
                in: 0...max(playback.duration, 0.01)
            )
            .tint(theme.accentColor)
            .accessibilityLabel("Playback progress")
            .accessibilityValue(
                "\(playbackTimeText(playback.currentTime)) of \(playbackTimeText(playback.duration))"
            )
            .accessibilityIdentifier("dictation_progress_\(playback.playingCaptureId ?? 0)")

            HStack(spacing: 10) {
                Text(playbackTimeText(playback.currentTime))
                Spacer(minLength: 4)
                Button {
                    isPlaybackSpeedPickerPresented = true
                } label: {
                    Label(playbackRateText(playback.playbackRate), systemImage: "speedometer")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Theme.textPrimary)
                }
                .confirmationDialog(
                    "Playback speed",
                    isPresented: $isPlaybackSpeedPickerPresented,
                    titleVisibility: .visible
                ) {
                    ForEach(DictationAudioPlaybackController.supportedRates, id: \.self) { rate in
                        Button {
                            playback.setRate(rate)
                        } label: {
                            HStack {
                                Text(playbackRateText(rate))
                                    .foregroundStyle(Theme.textPrimary)
                                if playback.playbackRate == rate {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(Theme.textPrimary)
                                }
                            }
                        }
                        .accessibilityIdentifier("dictation_speed_option_\(playbackRateText(rate))")
                    }
                }
                .tint(Theme.textPrimary)
                .accessibilityLabel("Playback speed")
                .accessibilityIdentifier("dictation_speed_\(playback.playingCaptureId ?? 0)")
                Spacer(minLength: 4)
                Text("-\(playbackTimeText(max(0, playback.duration - playback.currentTime)))")
            }
            .font(.caption)
            .foregroundStyle(Theme.textSecondary)
        }
        .padding(.leading, 46)
    }

    private func dateText(for capture: DictationCaptureItem) -> String {
        Date(timeIntervalSince1970: TimeInterval(capture.capturedAtMillis) / 1000)
            .formatted(date: .abbreviated, time: .shortened)
    }

    private func durationText(_ durationMillis: Int64) -> String {
        let totalSeconds = max(0, durationMillis / 1000)
        return String(format: "%d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    private func playbackTimeText(_ time: TimeInterval) -> String {
        durationText(Int64(max(0, time) * 1000))
    }

    private func playbackRateText(_ rate: Float) -> String {
        switch rate {
        case 1.0: return "1x"
        case 1.5: return "1.5x"
        case 2.0: return "2x"
        default: return "\(rate)x"
        }
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

        if playback.playingCaptureId == capture.id {
            stopPlayback()
            return
        }

        stopPlayback()
        do {
            try playback.play(path: path, captureId: capture.id)
        } catch {
            playbackError = "This recording could not be opened."
        }
    }

    private func stopPlayback() {
        playback.stop()
    }
}

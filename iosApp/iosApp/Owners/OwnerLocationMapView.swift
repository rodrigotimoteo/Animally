import MapKit
import SwiftUI

/// Presentation-only coordinate adapter for MapKit.
private struct OwnerMapCoordinate {
    let latitude: Double
    let longitude: Double

    init?(
        latitude: Double?,
        longitude: Double?
    ) {
        guard let latitude,
              let longitude,
              latitude.isFinite,
              longitude.isFinite,
              (-90.0...90.0).contains(latitude),
              (-180.0...180.0).contains(longitude)
        else {
            return nil
        }
        self.latitude = latitude
        self.longitude = longitude
    }

    init(latitude: Double, longitude: Double) {
        self.latitude = latitude
        self.longitude = longitude
    }

    var clCoordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
}

/// A small, non-editable map preview with an optional Apple Maps action.
struct OwnerLocationPreview: View {
    private let title: String
    private let coordinate: OwnerMapCoordinate
    private let accentColor: Color
    private let showsOpenInMaps: Bool
    @State private var cameraPosition: MapCameraPosition

    init(
        title: String,
        latitude: Double,
        longitude: Double,
        accentColor: Color,
        showsOpenInMaps: Bool = true
    ) {
        let coordinate = OwnerMapCoordinate(latitude: latitude, longitude: longitude)
        self.title = title
        self.coordinate = coordinate
        self.accentColor = accentColor
        self.showsOpenInMaps = showsOpenInMaps
        _cameraPosition = State(
            initialValue: .region(
                MKCoordinateRegion(
                    center: coordinate.clCoordinate,
                    span: MKCoordinateSpan(latitudeDelta: 0.02, longitudeDelta: 0.02)
                )
            )
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Map(position: $cameraPosition) {
                Marker(title, systemImage: "mappin", coordinate: coordinate.clCoordinate)
            }
            .frame(height: 170)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .allowsHitTesting(false)

            if showsOpenInMaps {
                Button(action: openInMaps) {
                    Label("Open in Maps", systemImage: "arrow.up.forward.app")
                        .font(.subheadline.weight(.semibold))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .tint(accentColor)
            }
        }
    }

    private func openInMaps() {
        let mapItem = MKMapItem(placemark: MKPlacemark(coordinate: coordinate.clCoordinate))
        mapItem.name = title
        mapItem.openInMaps(launchOptions: nil)
    }
}

/// MapKit picker that confirms the coordinate under a fixed center pin.
struct OwnerLocationPickerView: View {
    @Environment(\.dismiss) private var dismiss

    private let initialCoordinate: OwnerMapCoordinate?
    private let initialAddress: String?
    private let accentColor: Color
    private let onSelect: (Double, Double) -> Void
    @State private var cameraPosition: MapCameraPosition
    @State private var mapCenter: CLLocationCoordinate2D
    @State private var searchText = ""
    @State private var searchResults: [MKMapItem] = []
    @State private var isSearching = false
    @State private var searchMessage: String?
    @State private var didGeocodeInitialAddress = false

    init(
        initialLatitude: Double?,
        initialLongitude: Double?,
        address: String?,
        accentColor: Color,
        onSelect: @escaping (Double, Double) -> Void
    ) {
        let coordinate = OwnerMapCoordinate(
            latitude: initialLatitude,
            longitude: initialLongitude
        )
        let center = coordinate?.clCoordinate ?? CLLocationCoordinate2D(latitude: 0, longitude: 0)
        let span = coordinate == nil
            ? MKCoordinateSpan(latitudeDelta: 60, longitudeDelta: 60)
            : MKCoordinateSpan(latitudeDelta: 0.02, longitudeDelta: 0.02)
        let trimmedAddress = address?.trimmingCharacters(in: .whitespacesAndNewlines)

        self.initialCoordinate = coordinate
        self.initialAddress = trimmedAddress?.isEmpty == false ? trimmedAddress : nil
        self.accentColor = accentColor
        self.onSelect = onSelect
        _cameraPosition = State(
            initialValue: .region(
                MKCoordinateRegion(center: center, span: span)
            )
        )
        _mapCenter = State(initialValue: center)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                searchControls

                ZStack {
                    Map(position: $cameraPosition)
                        .onMapCameraChange(frequency: .continuous) { context in
                            mapCenter = context.region.center
                        }

                    Image(systemName: "mappin.circle.fill")
                        .font(.system(size: 42))
                        .foregroundStyle(accentColor)
                        .shadow(color: .black.opacity(0.25), radius: 4, y: 2)
                        .offset(y: -20)
                        .allowsHitTesting(false)
                }
                .overlay(alignment: .bottom) {
                    Text("Move the map until the pin is at the owner's location")
                        .font(.caption)
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(.black.opacity(0.62), in: Capsule())
                        .padding(.bottom, 12)
                }

                Button {
                    onSelect(mapCenter.latitude, mapCenter.longitude)
                    dismiss()
                } label: {
                    Label("Use this location", systemImage: "checkmark.circle.fill")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(accentColor)
                .padding()
            }
            .navigationTitle("Owner location")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
        .task {
            await geocodeInitialAddressIfNeeded()
        }
    }

    private var searchControls: some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) {
                TextField("Search address or place", text: $searchText)
                    .textFieldStyle(.roundedBorder)
                    .submitLabel(.search)
                    .onSubmit(search)

                Button(action: search) {
                    if isSearching {
                        ProgressView()
                    } else {
                        Image(systemName: "magnifyingglass")
                    }
                }
                .buttonStyle(.bordered)
                .tint(accentColor)
                .accessibilityLabel("Search map")
            }

            if !searchResults.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Array(searchResults.enumerated()), id: \.offset) { _, item in
                            Button {
                                select(item)
                            } label: {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(item.name ?? item.placemark.name ?? "Location")
                                        .font(.caption.weight(.semibold))
                                        .lineLimit(1)
                                    if let subtitle = item.placemark.title {
                                        Text(subtitle)
                                            .font(.caption2)
                                            .lineLimit(1)
                                    }
                                }
                                .frame(width: 180, alignment: .leading)
                            }
                            .buttonStyle(.bordered)
                            .tint(accentColor)
                        }
                    }
                    .padding(.horizontal, 1)
                }
            }

            if let searchMessage {
                Text(searchMessage)
                    .font(.caption)
                    .foregroundStyle(Theme.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding()
        .background(Theme.surfaceElevated)
    }

    private func search() {
        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else {
            searchMessage = "Enter an address or place to search."
            searchResults = []
            return
        }

        Task { @MainActor in
            isSearching = true
            searchMessage = nil
            defer { isSearching = false }

            let request = MKLocalSearch.Request()
            request.naturalLanguageQuery = query

            do {
                let response = try await MKLocalSearch(request: request).start()
                searchResults = Array(response.mapItems.prefix(5))
                if searchResults.isEmpty {
                    searchMessage = "No locations found."
                }
            } catch {
                searchResults = []
                searchMessage = "Map search is unavailable right now."
            }
        }
    }

    private func select(_ item: MKMapItem) {
        guard let coordinate = item.placemark.location?.coordinate else {
            searchMessage = "That result has no usable map location."
            return
        }
        mapCenter = coordinate
        cameraPosition = .region(
            MKCoordinateRegion(
                center: coordinate,
                span: MKCoordinateSpan(latitudeDelta: 0.02, longitudeDelta: 0.02)
            )
        )
        searchResults = []
        searchMessage = nil
    }

    @MainActor
    private func geocodeInitialAddressIfNeeded() async {
        guard !didGeocodeInitialAddress,
              initialCoordinate == nil,
              let initialAddress
        else {
            return
        }
        didGeocodeInitialAddress = true

        let request = MKLocalSearch.Request()
        request.naturalLanguageQuery = initialAddress
        do {
            let response = try await MKLocalSearch(request: request).start()
            if let firstResult = response.mapItems.first {
                select(firstResult)
            }
        } catch {
            // The user can still search manually or select the map center.
        }
    }
}

import SwiftUI
import Shared

struct OwnerListView: View {
    @StateObject private var viewModel = OwnerListViewModel()
    @State private var searchText = ""

    var body: some View {
        Group {
            if viewModel.state.isLoading && viewModel.state.owners.isEmpty {
                loadingView
            } else if viewModel.state.owners.isEmpty {
                emptyView
            } else {
                listView
            }
        }
        .navigationTitle("Owners")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink(value: Route.ownerEdit(nil)) {
                    Image(systemName: "plus")
                        .accessibilityLabel("Add owner")
                }
            }
        }
        .overlay(alignment: .top) {
            if let errorMessage = viewModel.state.errorMessage {
                errorBanner(message: errorMessage)
            }
        }
        .onAppear {
            viewModel.load()
        }
        .searchable(text: $searchText, prompt: "Search owners")
        .onChange(of: searchText) { _, newValue in
            viewModel.setSearchQuery(newValue)
        }
    }

    private var listView: some View {
        List {
            if displayedOwners.isEmpty {
                ContentUnavailableView.search(text: searchText)
            } else {
                Section {
                    ForEach(displayedOwners, id: \.id) { owner in
                        NavigationLink(value: Route.ownerDetail(owner.id)) {
                            OwnerRowView(owner: owner)
                        }
                        .accessibilityIdentifier("owner_row_\(owner.id)")
                        .confirmationSwipeDelete(
                            title: owner.name,
                            message: "Owners with linked patients cannot be deleted. Unlink their patients first."
                        ) {
                            viewModel.delete(ownerId: owner.id)
                        }
                    }
                }
                header: {
                    Text("\(displayedOwners.count) owner\(displayedOwners.count == 1 ? "" : "s")")
                        .textCase(nil)
                }
            }
        }
        .listStyle(.insetGrouped)
        .refreshable {
            viewModel.load()
        }
    }

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)
            Text("Loading owners…")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var emptyView: some View {
        VStack(spacing: 20) {
            Image(systemName: "person.crop.circle")
                .font(.system(size: 64))
                .foregroundStyle(Theme.forestGreen.opacity(0.6))
            Text("No owners yet")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Theme.textPrimary)
            Text("Tap + to add one")
                .font(.subheadline)
                .foregroundStyle(Theme.textSecondary)
            NavigationLink(value: Route.ownerEdit(nil)) {
                Label("Add your first owner", systemImage: "plus")
                    .font(.subheadline.weight(.semibold))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func errorBanner(message: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(Theme.amber)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Theme.textPrimary)
                .lineLimit(2)
            Spacer()
            Button {
                viewModel.dismissError()
            } label: {
                Image(systemName: "xmark")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Theme.textSecondary)
                    .accessibilityLabel("Dismiss error")
            }
        }
        .padding(12)
        .background(Theme.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.08), radius: 8, y: 2)
        .padding(.horizontal)
        .padding(.top, 8)
        .transition(.move(edge: .top).combined(with: .opacity))
    }

    private var displayedOwners: [Owner_] {
        viewModel.state.visibleOwners
    }
}

struct OwnerRowView: View {
    let owner: Owner_

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "person.crop.circle.fill")
                .font(.title2)
                .foregroundStyle(Theme.forestGreen)
                .frame(width: 44, height: 44)
                .background(Theme.forestGreen.opacity(0.12))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(owner.name)
                    .font(.headline)
                    .foregroundStyle(Theme.textPrimary)

                if let phone = owner.phone, !phone.isEmpty {
                    Text(phone)
                        .font(.subheadline)
                        .foregroundStyle(Theme.textSecondary)
                } else if let email = owner.email, !email.isEmpty {
                    Text(email)
                        .font(.subheadline)
                        .foregroundStyle(Theme.textSecondary)
                } else {
                    Text("No contact details")
                        .font(.subheadline)
                        .foregroundStyle(Theme.textTertiary)
                }
            }

            Spacer()
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Owner \(owner.name)")
        .accessibilityHint("Opens owner details")
    }
}

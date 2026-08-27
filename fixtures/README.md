# Demo equine herd

`demo-equine-herd.json` is a fictional, schema-v1 Animally backup for local
QA and assistant evaluation. It contains four horses, three fictional owners,
pregnancy/reproduction records, preventive care, weights, and a small lameness
case. It contains no real personal or clinical data.

To load it in the iOS app, open **Settings → Restore Backup** and select the
file. Restoring replaces the current local database, so export a personal
backup first if the simulator or device contains anything you want to keep.

The fixture is intentionally conservative: only Lua do Pinhal and Estrela da
Serra have active positive gestation records; Orion do Vale carries the
lameness/medication facts; Brisa do Atlântico has an explicitly failed
breeding cycle.

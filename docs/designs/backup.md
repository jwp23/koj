# Backup

Backup protects the data against a lost or replaced phone. It is not sync: one device writes,
and a restore replaces everything.

## Destination

The user picks a folder once through Android's system file picker. Any storage app that plugs
into the picker works; Google Drive is the expected choice. The app holds no cloud credentials.

## File

Each backup is one JSON file holding the complete history: every source, drink, check-in, and
setting. The file carries a format version number.

## Schedule

The app writes a backup once a day when there is new data. It keeps the seven most recent files
and deletes older ones, so a bad backup cannot replace the only good copy.

If the storage app does not allow writes while koj is closed, the app writes the backup when it
is next opened instead.

## Restore

The user picks a backup file. The app replaces its data with the file's contents.

- A file in an older format is upgraded as it is read.
- A file in a newer format than the app understands is refused with a message, and no data
  changes.

## Staleness

Settings shows the time of the last successful backup. The Today screen shows a warning when
that time is more than two days old.

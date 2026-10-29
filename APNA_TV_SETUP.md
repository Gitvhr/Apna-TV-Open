# Apna TV Open — Project Setup

This repository is the Apna TV customization workspace.

## Upstream foundation
- OpenTV source: https://github.com/opentvproject/opentv
- Upstream project is licensed under GPL-3.0-or-later. Preserve the license and applicable notices when importing or distributing modified source.

## Planned work
1. Import the upstream source and its license/notices.
2. Confirm the baseline Android build works in GitHub Actions.
3. Apply Apna TV branding and Android TV remote-friendly UI customizations.
4. Test M3U playlist parsing and HLS/DASH playback with streams the user is authorized to access.
5. Add and test features incrementally, including EPG and favourites.

## Security
Do not commit playlist credentials, expiring stream URLs, cookies, or DRM keys. Only use streams and DRM keys you are authorized to access.

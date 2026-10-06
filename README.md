# Dokka-tools

[![Ubuntu build][ubuntu-build-badge]][gh-actions]
[![codecov][codecov-badge]][codecov] &nbsp;
[![license][license-badge]][license]

[gh-actions]: https://github.com/SpineEventEngine/dokka-tools/actions
[ubuntu-build-badge]: https://github.com/SpineEventEngine/dokka-tools/actions/workflows/build-on-ubuntu.yml/badge.svg
[codecov]: https://codecov.io/github/SpineEventEngine/dokka-tools
[codecov-badge]: https://codecov.io/github/SpineEventEngine/dokka-tools/graph/badge.svg
[license]: http://www.apache.org/licenses/LICENSE-2.0
[license-badge]: https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat

This repository contains tools for working with Dokka:

* [Dokka Extensions](dokka-extensions/README.md) - Module for custom Dokka plugins. There is
  the `ExcludeInternalPlugin` that excludes code annotated by `@Internal` from documentation,
  and the `NoteTagsPlugin` that renders the Javadoc tags `@apiNote`, `@implSpec`,
  and `@implNote`.

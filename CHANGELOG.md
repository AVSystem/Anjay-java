# Changelog

## 2.14.1 (December 29, 2021)

- Update Anjay to 2.14.1
- Add AnjayEventLoop object corresponding to Anjay event loop implementation
- Add AnjayIpsoButton, AnjayBasicIpsoSensor and Anjay3dIpsoSensor objects
  similar to Anjay IPSO objects implementation

## 2.11.0 (May 12, 2021)

- Update Anjay to 2.11.0
- Add tests for Observe attributes, security modes, setting offline mode and
  reconnecting
- Add AnjayAccessControl class
- Fix binding to the specific port
- Fix build on macOS
- Fix throwing exceptions from native code, now they're translated to Exception
  instead of Error
- Fix demo arguments description
- Fix race condition that could lead to crashes when shutting down the library

## 2.8.0.1 (Dec 1, 2020)

- Update Anjay to 2.8.0
- Reorganize project structure
- Configure publishing of packages to Maven Central

## 2.7.0 (Oct 23, 2020)

- Initial release using Anjay 2.7.0

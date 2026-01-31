# Architecture and Technical Descisions

_This document explains architectural and technical decisions for Tissue. It will mostly cover what and why._

> [!WARNING]
> This project is under active development.
> The documentation can go through a massive change anytime.

## Built With

- Backend
  - Java 21 (will migrate to GraalVM)
  - Lombok
  - Sprint Boot 3.3.3
  - Spring Data JPA (Hibernate)
  - Spring
  - Spring Security 6
  - Spring-Retry
  - Redis
  - PostgreSQL

- TUI
  - Python 3.10
  - [Textual](https://github.com/textualize/textual/)
  - [SQLite](https://sqlite.org/)

- Static Analysis and Linting
  - Checkstyle
  - Spotless
  - NullAway
  - ErrorProne

## Code Design

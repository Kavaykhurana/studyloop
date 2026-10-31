# Third-party components and attribution

NexHack 3.0 allows existing libraries, APIs, frameworks and open-source components with appropriate attribution, and
asks for significant AI assistance to be disclosed. This file is that attribution; AI assistance is disclosed in
[AI_USAGE.md](AI_USAGE.md). Licences were read from the resolved packages (`frontend/node_modules`, the Maven
dependency tree), not assumed. Everything listed is used as a dependency under its own licence; none of its source is
copied into this repository.

What is original to this project: the product concept and the loop design, the matching formula, the database schema,
the Gemini prompts and JSON schemas, the application code in `backend/` and `frontend/` (produced with the AI assistance
described in AI_USAGE.md), the seeded campus content, the logo and the documentation.

## External services and APIs

| Service | Used for | Terms |
|---|---|---|
| Google Gemini API: `gemini-3.5-flash-lite`, `gemini-3-flash-preview`, `gemini-embedding-2` | Doubt classification, hint ladder, snap (vision), study planner, embeddings for the duplicate radar, matching and hint grounding | Google Gemini API Terms of Service |
| Render | Hosting the Docker web service | Render Terms of Service |
| Supabase | Managed PostgreSQL with pgvector | Supabase Terms of Service |
| GitHub Actions | CI and the keepalive / smoke-test workflow | GitHub Terms of Service |
| Google Fonts | Serving the three typefaces below | Google Fonts Terms; fonts under their own licences |

## Database

| Component | Licence |
|---|---|
| PostgreSQL | PostgreSQL Licence |
| pgvector (vector type, HNSW index, cosine distance) | PostgreSQL Licence |
| citext (case-insensitive text, PostgreSQL contrib) | PostgreSQL Licence |

## Backend (Java 21, Maven)

| Component | Version | Licence |
|---|---|---|
| Spring Boot (web MVC, data JPA, JDBC, validation, actuator) | 4.1.2 | Apache-2.0 |
| Spring Security, OAuth2 Resource Server | 7.1.2 | Apache-2.0 |
| Nimbus JOSE + JWT | 10.9.1 | Apache-2.0 |
| Hibernate ORM | 7.4.13 | Apache-2.0 |
| HikariCP | 7.0.2 | Apache-2.0 |
| Flyway (core, PostgreSQL support) | 12.4.0 | Apache-2.0 |
| PostgreSQL JDBC driver | 42.7.14 | BSD-2-Clause |
| Jackson (`tools.jackson`, Jackson 3) | 3.1.7 | Apache-2.0 |
| springdoc-openapi (with Swagger UI 5.34.0) | 3.2.0 | Apache-2.0 |
| Lombok | 1.18.48 | MIT |
| JUnit Jupiter (tests) | 6.0.3 | EPL-2.0 |
| Mockito (tests) | 5.23.0 | MIT |
| AssertJ (tests) | 3.27.7 | Apache-2.0 |
| Testcontainers, incl. PostgreSQL module (tests) | 2.0.5 | MIT |

## Frontend (React, npm)

| Component | Version | Licence |
|---|---|---|
| React, React DOM | 19.3.1 | MIT |
| React Router | 7.18.5 | MIT |
| TanStack Query | 5.105.0 | MIT |
| Axios | 1.21.0 | MIT |
| react-hook-form, @hookform/resolvers | 7.91.0, 7.0.0 | MIT |
| zod | 4.6.7 | MIT |
| react-markdown, remark-gfm, rehype-sanitize, rehype-highlight | 10.1.0, 4.0.1, 6.0.0, 7.0.2 | MIT |
| highlight.js (incl. the `github.css` theme) | 11.12.0 | BSD-3-Clause |
| Recharts | 3.10.1 | MIT |
| lucide-react (icons) | 1.66.0 | ISC |
| sonner (toasts) | 2.0.8 | MIT |
| clsx, date-fns | 2.1.1, 4.4.0 | MIT |
| Tailwind CSS, @tailwindcss/vite | 4.3.3 | MIT |
| Vite, @vitejs/plugin-react | 8.3.5, 6.1.2 | MIT |
| TypeScript | 6.0.3 | Apache-2.0 |
| ESLint, typescript-eslint, eslint-plugin-react-hooks | 10.14.0, 8.72.1, 7.1.1 | MIT |

## Typefaces

| Font | Use | Licence |
|---|---|---|
| Bricolage Grotesque | Headings | SIL Open Font License 1.1 |
| Inter | Body text | SIL Open Font License 1.1 |
| JetBrains Mono | Code and numbers | SIL Open Font License 1.1 |

## Container images and build tooling

| Image or tool | Use | Licence |
|---|---|---|
| `node:22-alpine` (Node.js) | Building the frontend in the Docker image | MIT (Node.js) |
| `maven:3.9-eclipse-temurin-21` (Apache Maven, Maven Wrapper) | Building the backend | Apache-2.0 |
| `eclipse-temurin:21-jre` (Eclipse Temurin OpenJDK) | Running the app | GPL-2.0 with Classpath Exception |
| `pgvector/pgvector:pg16` | Local database and integration tests | PostgreSQL Licence |

Transitive dependencies of the components above are used under their respective licences; the full trees are in
`frontend/package-lock.json` and via `./mvnw dependency:tree` in `backend/`.

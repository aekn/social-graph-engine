# Social Graph Engine

A Java console social-networking application backed by **Neo4j** (Aura), built on top of the
[SNAP ego-Facebook](https://snap.stanford.edu/data/ego-Facebook.html) dataset. Implements 11 use
cases covering user management, social-graph features, and search/exploration.

## Architecture

- **Front-end:** Java 21 console app (`com.socialgraph.cli.ConsoleApp`).
- **Back-end:** Neo4j 5.x via the official `neo4j-java-driver` 6.x.
- **Schema:**
  - `(:User {id, username, email, name, bio, passwordHash, createdAt})`
  - `(:User)-[:FOLLOWS {since}]->(:User)` (directed)
- **Constraints / index:** unique `User.id`, `User.username`, `User.email`; range index on `User.name`.

## Prerequisites

- JDK 21 or newer
- Maven 3.9+
- A Neo4j AuraDB (or local Neo4j 5.x) instance

On macOS:

```bash
brew install openjdk@21 maven
```

## Configuration

Copy `.env.example` to `.env` and fill in your Aura connection:

```dotenv
NEO4J_URI=neo4j+s://<your-instance>.databases.neo4j.io
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=<password>
# Optional for Aura. Leave empty to use the server default database.
NEO4J_DATABASE=
```

## Build

```bash
mvn -q clean package
```

This produces `target/social-graph-engine-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## Loading the dataset (one-time)

Download SNAP ego-Facebook from [SNAP](https://snap.stanford.edu/data/ego-Facebook.html), extract it,
and place the extracted files in a local `facebook/` directory at the repository root
(`facebook/0.edges`, `facebook/107.edges`, etc.). The dataset folder is intentionally not committed.

Each undirected friendship is imported as **two** directed `FOLLOWS` edges (A→B and B→A),
preserving Facebook's mutual-friendship semantics while satisfying the assignment's directed-edge
requirement.

```bash
mvn -q exec:java -Dexec.args="--load --reset"
```

Expected output: ~4,039 users and ~176,000 `FOLLOWS` edges.

## Running the app

```bash
mvn -q exec:java
```

or, after `mvn package`:

```bash
java -jar target/social-graph-engine-1.0-SNAPSHOT-jar-with-dependencies.jar
```

The console walks you through the pre-login menu (register / login) and then the post-login menu
covering UC-3 through UC-11.

### Demo accounts

Every imported user has username `user_<id>` (e.g. `user_0`, `user_107`) and a default password of
`password123` — useful for graders to log in without registering. New users created via UC-1 use
their own chosen password.

## Repository layout

```
src/main/java/com/socialgraph
├── Main.java                       # entry point (--load / --reset / --data)
├── Config.java                     # dotenv-backed config
├── DatabaseService.java            # driver wrapper + schema bootstrap
├── model/User.java
├── repository/
│   ├── UserRepository.java         # CRUD, search, credentials lookup
│   └── GraphRepository.java        # follow/unfollow/recommend/popular/mutual
├── service/
│   ├── AuthService.java            # BCrypt register + login
│   └── SocialService.java          # CLI-facing orchestration
├── cli/ConsoleApp.java             # menus for all 11 use cases
└── loader/DatasetLoader.java       # SNAP ego-Facebook -> Neo4j
```

## Submission packaging (ZIP)

Create a clean source-code zip (excluding secrets, caches, and local tooling):

```bash
zip -r source-code.zip . \
  -x "*.git*" ".env" ".m2/*" "target/*" "tools/*" "*.DS_Store"
```

If your course expects `projects.zip` containing both `report.pdf` and source code:

```bash
mkdir -p submission/source-code
rsync -a . submission/source-code/ \
  --exclude ".git" --exclude ".env" --exclude ".m2" --exclude "target" --exclude "tools" --exclude ".DS_Store"
cp /path/to/report.pdf submission/report.pdf
cd submission && zip -r ../projects.zip .
```

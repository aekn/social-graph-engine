# Social Graph Engine

Graph Database Project using **Neo4j Aura + Java**.  
This application models a social network with graph-native features such as follow/unfollow, mutual connections, recommendation traversal, search, and popularity ranking.

## Team Information

- **Member 1:** Thilak Shekhar Shriyan (`thilak.shriyan@sjsu.edu`)
- **Member 2:** `<Add Name>` (`<add_email>`)
- **Member 3:** `<Add Name>` (`<add_email>`)

Repository: `social-graph-engine`

## Technology Stack

- Front-end: Java console interface
- Back-end: Neo4j Aura (cloud)
- Driver: Neo4j Java Driver `6.0.5`
- Build tool: Maven

## Property Graph Schema

- **Node:** `(:User {id, username, email, name, bio, passwordHash, createdAt})`
- **Relationship:** `(:User)-[:FOLLOWS {since}]->(:User)`

### Constraints and Index

```cypher
CREATE CONSTRAINT user_id IF NOT EXISTS
  FOR (u:User) REQUIRE u.id IS UNIQUE;

CREATE CONSTRAINT user_username IF NOT EXISTS
  FOR (u:User) REQUIRE u.username IS UNIQUE;

CREATE CONSTRAINT user_email IF NOT EXISTS
  FOR (u:User) REQUIRE u.email IS UNIQUE;

CREATE INDEX user_name IF NOT EXISTS
  FOR (u:User) ON (u.name);
```

## Dataset

- Source: [SNAP ego-Facebook](https://snap.stanford.edu/data/ego-Facebook.html)
- Ego networks used: `0, 107, 348, 414, 686, 698, 1684, 1912, 3437, 3980`
- Local folder: `facebook/`

Each undirected friendship `{a, b}` is loaded as two directed edges:
- `(a)-[:FOLLOWS]->(b)`
- `(b)-[:FOLLOWS]->(a)`

### Final Loaded Graph Size

- Users (nodes): **4,039**
- FOLLOWS (relationships): **176,468**

This exceeds the assignment minimum requirements (1,000 nodes / 5,000 relationships).

## Implemented Use Cases

All 11 required use cases are implemented:

1. User Registration
2. User Login
3. View Profile
4. Edit Profile
5. Follow Another User
6. Unfollow a User
7. View Following and Followers
8. Mutual Connections
9. Friend Recommendations
10. Search Users
11. Explore Popular Users

## Prerequisites

- JDK 21+
- Maven 3.9+
- Neo4j AuraDB instance (or local Neo4j 5.x)

## Environment Configuration

Copy `.env.example` to `.env` and update values:

```dotenv
NEO4J_URI=neo4j+s://<your-instance>.databases.neo4j.io
NEO4J_USERNAME=<your-username>
NEO4J_PASSWORD=<your-password>
NEO4J_DATABASE=
```

Leave `NEO4J_DATABASE` empty to use Aura default database.

## Build and Run

### Build

```bash
mvn -q clean compile
```

### Load Dataset (one-time)

```bash
mvn -q exec:java -Dexec.args="--load --reset"
```

### Run Application

```bash
mvn -q exec:java
```

### Demo Accounts

Imported users follow this format:
- Username: `user_<id>` (example: `user_0`, `user_107`)
- Password: `password123`

Newly registered users use their own password from UC-1.

## Repository Structure

```text
src/main/java/com/socialgraph
├── Main.java
├── Config.java
├── DatabaseService.java
├── model/User.java
├── repository/
│   ├── UserRepository.java
│   └── GraphRepository.java
├── service/
│   ├── AuthService.java
│   └── SocialService.java
├── cli/ConsoleApp.java
└── loader/DatasetLoader.java
```

## Submission-Ready Checklist

- [ ] Fill Team Information placeholders in this README.
- [ ] Ensure your `report.pdf` is finalized separately.
- [ ] Confirm `.env` is not included in submission.
- [ ] Confirm project builds and runs successfully.
- [ ] Verify all 11 use cases are demonstrable.

## Create Clean ZIP for Submission

Create source-only zip:

```bash
zip -r source-code.zip . \
  -x "*.git*" ".env" ".m2/*" "target/*" "tools/*" "*.DS_Store" "*.zip" "submission/*"
```

If submission requires one `projects.zip` containing both `report.pdf` and source code:

```bash
mkdir -p submission/source-code
rsync -a . submission/source-code/ \
  --exclude ".git" --exclude ".env" --exclude ".m2" --exclude "target" --exclude "tools" --exclude ".DS_Store" --exclude "*.zip" --exclude "submission"
cp /path/to/report.pdf submission/report.pdf
cd submission && zip -r ../projects.zip .
```

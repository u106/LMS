# ADR-007: Amazon S3 for lesson file storage

**Status:** Accepted
**Date:** 2026-08-24

## Context

Lessons need attached files - video, slides, PDFs. Module 2's PRD (US-7)
deliberately left the storage location open, because the choice affects
deployment more than it affects application code.

Three options were considered.

**A. The local filesystem.** Simplest to build and needs no external account.
But the files are tied to one machine: they disappear on redeploy, do not
survive a container restart, and cannot be read by a second instance of the
app. Backups become a separate problem. It works right up until the moment
the project stops being a single process on one laptop.

**B. Bytes in MySQL.** Keeps everything in one place and inside one
transaction. But it inflates the database and its backups with data the
database cannot query, pushes large blobs through the connection pool, and
makes every restore slower. Widely regarded as the wrong tool for this.

**C. Amazon S3.** Purpose-built for this: durable, effectively unbounded,
independent of any one server, and able to serve bytes directly to a client
without the application relaying them. Costs an AWS account and a small
amount of money past the free tier.

## Decision

Use Amazon S3. The database stores file *metadata* only - an object key,
the original filename, content type, and size. The bytes live in S3.

Access goes through a `FileStorage` interface in the shared kernel, with an
`S3FileStorage` implementation. Calling code never imports the AWS SDK.

The bucket stays private with Block Public Access on. Files reach the
browser through short-lived presigned URLs rather than public objects or
bytes proxied through the application.

Object keys are generated server-side as
`courses/{courseId}/lessons/{lessonId}/{uuid}.{ext}` - never derived from a
client-supplied filename.

Credentials are resolved through the AWS SDK's default provider chain:
environment variables locally, an IAM role in production. Nothing is written
into `application.yml` or committed.

## Consequences

**Positive**
- Files survive redeploys, restarts, and horizontal scaling
- The application never streams large files through its own memory or
  bandwidth - presigned URLs let the browser talk to S3 directly
- The `FileStorage` interface means the local-disk fallback of option A
  remains a one-class change if ever needed
- Teaches genuinely transferable AWS skills: IAM least privilege, presigned
  URLs, the credential chain

**Negative**
- Following along now requires an AWS account, which is a real barrier for
  some viewers. LocalStack is a free local substitute, mentioned but not
  covered
- Costs real money past the free tier, so a billing alert is mandatory
- Deleting a lesson or course no longer cleans up completely on its own:
  `ON DELETE CASCADE` removes lesson rows but knows nothing about S3
  objects. Orphaned files must be deleted explicitly in application code -
  a genuine cost of splitting storage across two systems
- Uploads are no longer transactional with the database. An upload can
  succeed and the row write can fail, leaving an unreferenced object

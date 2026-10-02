# Notes

## Day 1
- Installed: (list the tools you installed)
- Problems I hit and how I fixed them:
  - Git said "repository not found" because the repo was private and I wasn't signed in. Fixed by making it public.

### Answers
1. Browser / server / database:
2. Why the browser shouldn't decide if a slot is free:
3. What an API is:

### sql basics
1. What is the difference between a primary key and a foreign key?
2. What did the database do when you inserted a section for lot 99, and why is that useful?
3. What does a JOIN do?
4. In challenge 4, why did the second ACTIVE booking fail?

### answers
## SQL basics

### 1. Primary key vs foreign key
A primary key is a column (usually `id`) that uniquely identifies each row in its own table, so no two rows share it and it can never be empty. A foreign key is a column in one table that points to the primary key of another table, for example `slot.section_id` pointing to `section.id`. The primary key identifies a row, and the foreign key links rows across tables.

### 2. Inserting a section for lot 99
The database rejected the insert with a foreign key violation, because no lot with id 99 exists. This is useful because it stops "orphan" data, such as a section that belongs to a lot that doesn't exist. The database enforces this even if the application code has a bug.

### 3. What a JOIN does
A JOIN combines rows from two or more tables by matching a column, usually a foreign key to a primary key. For example, joining `slot` and `section` on `slot.section_id = section.id` lets me see each slot together with its section code in one result.

### 4. Why the second ACTIVE booking failed (challenge 4)
I created a partial unique index on `booking(slot_id)` that applies only where `status = 'ACTIVE'`. The first ACTIVE booking for a slot was allowed, but a second one for the same slot violated the index, so the database refused it. This is the safety net against double booking: even if two requests get past the application checks at the same time, the database allows only one active reservation per slot.

### Other notes
- A leftover `parking_slots` table from an earlier attempt was dropped to start clean.
- The `parking_user` role already existed from before, so I reset its password and made it the owner of `parking_db`.
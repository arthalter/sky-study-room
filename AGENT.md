# Sky Study Room Optimization Tasks

This file records the agreed optimization plan for the `sky-study-room` project.

## Phase 1: Build And Startup Stability

1. Fix Lombok compilation configuration.
2. Add explicit Maven Compiler configuration for Java 17 and Lombok annotation processing.
3. Verify backend build with `mvn clean test` and `mvn package`.
4. Add project startup documentation covering local startup, Docker startup, default accounts, URLs, and common issues.

## Phase 2: Reservation Consistency

5. Add a transaction boundary to reservation submission.
6. Add a transaction boundary to reservation review.
7. Add the conflict-detection composite index: `reservation(resource_id, reserve_date, status, start_time, end_time)`.
8. Add a pessimistic-lock query method for reservation records by ID.
9. Change review flow to lock the reservation before checking status.
10. Before approving a reservation, lock approved reservations for the same resource and date.
11. Extract reservation status transition validation into a centralized method.

## Phase 3: Validation

12. Reject reservations whose date is earlier than today.
13. Ensure reservation start time is earlier than end time through unified validation.
14. Validate that reservation time falls within the resource `open_time` range.
15. Add Bean Validation annotations to DTOs.
16. Add `@Valid` validation entry points in controllers.
17. Extend global exception handling for validation errors.

## Phase 4: Redis Usage

18. Cache resource category results in Redis.
19. Evict the resource category cache after resource create, update, or status changes.
20. Add a JWT token blacklist mechanism.
21. Connect the JWT interceptor to the Redis token blacklist check.

## Phase 5: Audit And Tests

22. Add a `reservation_audit_log` table.
23. Write audit logs when an admin approves or rejects a reservation.
24. Add core flow tests covering login, reservation submission, conflicts, approval/rejection, cancellation, unauthorized access, and invalid status transitions.

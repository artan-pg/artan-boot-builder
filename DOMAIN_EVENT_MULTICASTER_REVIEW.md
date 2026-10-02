# Review Notes — DomainEventMulticaster Interface

## Contract alignment with existing code (verified)
- `DomainEventSmartListener#getListenerId` Javadoc already promises: duplicate-ID rejection,
  removal by ID, interceptor logs referencing listenerId -> interface must expose these.
- `DomainEventInterceptor` callbacks take `(event, listenerId)` -> multicaster owns ID resolution
  (fallback to class name when blank).
- `DomainEventListener#supportsAsyncExecution()` exists -> async-capable multicaster needed.

## Issues & recommendations
1. Add `@NullMarked` at package-info; drop scattered `@NonNull`.
2. Naming symmetry: `add/removeDomainEventListener` vs `add/removeInterceptor` -> unify.
3. Missing capability queries: `getListenerCount()`, `hasListener(...)`, `getListenersForEvent(...)`.
4. Missing remove-by-id and interceptor ordering (`Ordered` or explicit order param).
5. `multicastEvent(event)` should document: no-listener policy, exception propagation semantics,
   reentrant registration safety, immutability of event during dispatch.
6. Consider overload `multicastEvent(event, EventType override)` for routing deserialized events.
7. Return type of add: consider boolean (false on duplicate id) instead of silent/exception-only.
8. Keep interface sync-default; async lives in a separate implementation, not the port.

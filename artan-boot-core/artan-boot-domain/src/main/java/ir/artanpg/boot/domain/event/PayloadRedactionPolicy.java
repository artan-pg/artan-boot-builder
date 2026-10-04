/*
 * Copyright (c) 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ir.artanpg.boot.domain.event;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Policy that decides how an event payload is rendered into a
 * human-readable string (typically for logging via
 * {@link Object#toString()}).
 *
 * <p>Domain event payloads may contain sensitive data (personal data,
 * credentials, financial amounts). Rendering them verbatim in logs creates
 * privacy and security problems. A redaction policy intercepts the
 * {@code toString()} pipeline and replaces the payload representation with a
 * safe placeholder when needed.
 *
 * <p>Policies are consulted per event instance, so subclasses can redact
 * selectively based on the concrete event type or payload content.
 *
 * <h2>Built-in policies:</h2>
 * <ul>
 *   <li>{@link #NONE} — render the payload as-is (default)</li>
 *   <li>{@link #MASKED} — never render the payload; print
 *       {@code "[REDACTED]"} instead</li>
 * </ul>
 *
 * @author Mohammad Yazdian
 * @see AbstractDomainEvent#redactionPolicy()
 * @since 0.1.0
 */
@FunctionalInterface
public interface PayloadRedactionPolicy {

    /**
     * The placeholder rendered in place of a redacted payload.
     */
    String REDACTED_PLACEHOLDER = "[REDACTED]";

    /**
     * Redacts the given payload into a safe string representation.
     *
     * @param payload the event payload to render; may be {@code null}
     *                only if the concrete event implementation allows it
     * @return the string to be printed instead of the raw payload;
     *         must not be {@code null}
     */
    @NonNull String redact(@Nullable Object payload);

    /**
     * A policy that performs no redaction and simply delegates to
     * {@link String#valueOf(Object)}.
     *
     * <p><b>Warning:</b> using this policy means the full payload content
     * will appear in logs. Prefer a masking policy for events carrying
     * sensitive data.
     */
    PayloadRedactionPolicy NONE = payload -> String.valueOf(payload);

    /**
     * A policy that always returns {@link #REDACTED_PLACEHOLDER}, hiding the
     * payload content entirely.
     */
    PayloadRedactionPolicy MASKED = payload -> REDACTED_PLACEHOLDER;
}

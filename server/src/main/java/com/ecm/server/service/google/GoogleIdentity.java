package com.ecm.server.service.google;

/** Verified, minimal Google identity used for account linking. */
public record GoogleIdentity(String subject, String email) {
}

package com.eprocure.pr.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class IpAddressUtilTest {

    @Test
    void should_return_first_ip_when_x_forwarded_for_has_multiple_ips() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 70.41.3.18, 150.172.238.178");

        String clientIp = IpAddressUtil.getClientIp(request);

        assertThat(clientIp).isEqualTo("203.0.113.195");
    }

    @Test
    void should_return_x_forwarded_for_when_single_ip() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195");

        String clientIp = IpAddressUtil.getClientIp(request);

        assertThat(clientIp).isEqualTo("203.0.113.195");
    }

    @Test
    void should_return_x_real_ip_when_x_forwarded_for_is_missing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("198.51.100.1");

        String clientIp = IpAddressUtil.getClientIp(request);

        assertThat(clientIp).isEqualTo("198.51.100.1");
    }

    @Test
    void should_fallback_to_remote_addr_when_all_headers_are_missing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String clientIp = IpAddressUtil.getClientIp(request);

        assertThat(clientIp).isEqualTo("127.0.0.1");
    }

    @Test
    void should_return_fallback_mask_when_request_is_null() {
        String clientIp = IpAddressUtil.getClientIp(null);

        assertThat(clientIp).isEqualTo("***");
    }
}

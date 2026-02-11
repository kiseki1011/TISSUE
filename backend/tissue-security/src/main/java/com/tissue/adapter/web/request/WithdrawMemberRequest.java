package com.tissue.adapter.web.request;

import jakarta.validation.constraints.Size;

public record WithdrawMemberRequest(@Size(max = 100) String password) {}

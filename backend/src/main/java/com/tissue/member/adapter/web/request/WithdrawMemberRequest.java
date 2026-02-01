package com.tissue.member.adapter.web.request;

import jakarta.validation.constraints.Size;

public record WithdrawMemberRequest(@Size(max = 100) String password) {}

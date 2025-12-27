package com.tissue.member.adapter.in.web.dto.request;

import jakarta.validation.constraints.Size;

public record WithdrawMemberRequest(@Size(max = 100) String password) {}

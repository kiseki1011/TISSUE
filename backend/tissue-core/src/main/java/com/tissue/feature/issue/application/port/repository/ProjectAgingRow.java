package com.tissue.feature.issue.application.port.repository;

/**
 * Bucketed counts of a project's open issues by age (time since work started, or since creation when not
 * yet started). The buckets partition every open issue, so their sum is the open total.
 */
public interface ProjectAgingRow {

    long getUnder3d();

    long getDays3to7();

    long getWeeks1to2();

    long getOver2w();
}

// Shared env / constants for k6 scenarios

export const BASE          = __ENV.BASE_URL      || 'http://app:8080';
export const IDENTIFIER    = __ENV.IDENTIFIER    || 'loadadmin@loadtest.local';
export const PASSWORD      = __ENV.PASSWORD      || 'Loadtest1!';
export const WORKSPACE_KEY = __ENV.WORKSPACE_KEY || 'WS0001';
export const TESTID        = __ENV.TESTID        || 'local';

// Seed shape (must match loadtest-seed.sql)
export const PROJECT_KEYS  = ['P0001','P0002','P0003','P0004','P0005',
                              'P0006','P0007','P0008','P0009','P0010'];
export const ISSUES_PER_PROJ = parseInt(__ENV.ISSUES_PER_PROJ || '10000');
export const MEMBER_COUNT    = parseInt(__ENV.MEMBER_COUNT    || '10000');

export const MEMBERS_PER_WS  = parseInt(__ENV.MEMBERS_PER_WS  || '100');

// Workflow ids accessible inside WS0001.
// seed.sql creates 2 workflows per project
export const WORKFLOW_ID_MIN   = parseInt(__ENV.WORKFLOW_ID_MIN   || '1');
export const WORKFLOW_ID_MAX   = parseInt(__ENV.WORKFLOW_ID_MAX   || '20');

// Seed creates one issue_type per project. For WS0001 (the workspace we test),
// only the first PROJ_PER_WS types are accessible. Other ids return 404.
export const ISSUE_TYPES_PER_WS = parseInt(__ENV.ISSUE_TYPES_PER_WS || '10');

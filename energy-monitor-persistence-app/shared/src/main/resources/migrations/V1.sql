CREATE TABLE energy_measurements (
    id uuid primary key,
    joules float8 not null,
    duration_seconds float8 not null,
    recorded_at timestamp with time zone not null,
    run int not null,
    organization text not null,
    repository text not null,
    branch text not null,
    tag text
);

CREATE INDEX IF NOT EXISTS energy_measurements_organization_idx ON energy_measurements (organization);

CREATE INDEX IF NOT EXISTS energy_measurements_repository_idx ON energy_measurements (repository);

CREATE INDEX IF NOT EXISTS energy_measurements_branch_idx ON energy_measurements (branch);

CREATE INDEX IF NOT EXISTS energy_measurements_run_idx ON energy_measurements (run);
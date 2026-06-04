param(
    [string] $EnvFile = ".env",
    [switch] $NoStartPostgres
)

$ErrorActionPreference = "Stop"

function Read-DotEnv {
    param([string] $Path)

    $values = @{}
    if (-not (Test-Path -LiteralPath $Path)) {
        return $values
    }

    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line.Length -eq 0 -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }
        $parts = $line.Split("=", 2)
        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $values[$name] = $value
    }
    return $values
}

function Get-ConfigValue {
    param(
        [hashtable] $DotEnv,
        [string] $Name,
        [string] $Default
    )

    $envValue = [Environment]::GetEnvironmentVariable($Name)
    if (-not [string]::IsNullOrWhiteSpace($envValue)) {
        return $envValue
    }
    if ($DotEnv.ContainsKey($Name) -and -not [string]::IsNullOrWhiteSpace($DotEnv[$Name])) {
        return $DotEnv[$Name]
    }
    return $Default
}

function Assert-SqlIdentifier {
    param(
        [string] $Value,
        [string] $Name
    )

    if (-not ($Value -match "^[A-Za-z_][A-Za-z0-9_]*$")) {
        throw "$Name must be a simple SQL identifier. Current value '$Value' is not supported by this maintenance script."
    }
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $repoRoot

$dotEnv = Read-DotEnv -Path $EnvFile

$postgresUser = Get-ConfigValue -DotEnv $dotEnv -Name "POSTGRES_ADMIN_USER" -Default "postgres"
$analyticsUser = Get-ConfigValue -DotEnv $dotEnv -Name "ANALYTICS_DB_USER" -Default "analytics_user"
$analyticsPass = Get-ConfigValue -DotEnv $dotEnv -Name "ANALYTICS_DB_PASS" -Default "analytics_pass_dev"
$analyticsDb = Get-ConfigValue -DotEnv $dotEnv -Name "ANALYTICS_DB_NAME" -Default "db_analytics"
$analyticsSchema = Get-ConfigValue -DotEnv $dotEnv -Name "ANALYTICS_DB_SCHEMA" -Default "analytics"

Assert-SqlIdentifier -Value $postgresUser -Name "POSTGRES_ADMIN_USER"
Assert-SqlIdentifier -Value $analyticsUser -Name "ANALYTICS_DB_USER"
Assert-SqlIdentifier -Value $analyticsDb -Name "ANALYTICS_DB_NAME"
Assert-SqlIdentifier -Value $analyticsSchema -Name "ANALYTICS_DB_SCHEMA"

if (-not $NoStartPostgres) {
    docker compose up -d postgres
}

$escapedAnalyticsPass = $analyticsPass.Replace("'", "''")

$sql = @"
\set ON_ERROR_STOP on

DO `$`$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '$analyticsUser') THEN
        EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', '$analyticsUser', '$escapedAnalyticsPass');
    END IF;
END
`$`$;

SELECT format('CREATE DATABASE %I OWNER %I', '$analyticsDb', '$analyticsUser')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = '$analyticsDb')\gexec

ALTER DATABASE $analyticsDb OWNER TO $analyticsUser;

\connect $analyticsDb

CREATE EXTENSION IF NOT EXISTS pgcrypto;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA IF NOT EXISTS $analyticsSchema AUTHORIZATION $analyticsUser;
ALTER SCHEMA $analyticsSchema OWNER TO $analyticsUser;
GRANT CONNECT ON DATABASE $analyticsDb TO $analyticsUser;
GRANT USAGE, CREATE ON SCHEMA $analyticsSchema TO $analyticsUser;
ALTER DATABASE $analyticsDb SET search_path TO $analyticsSchema;

SELECT 'analytics database is ready' AS status;
"@

$sql | docker compose exec -T postgres psql -U $postgresUser -d postgres

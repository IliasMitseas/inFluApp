# Reanalyze all posts via TestDataLoader and run the Python evaluation script
# Usage: Open PowerShell in project root and run: .\scripts\reanalyze_and_evaluate.ps1

Set-StrictMode -Version Latest

# Config
# Determine project root as the parent directory of this script's directory so the
# script works when invoked from any working directory (e.g., scheduled tasks or different shells).
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$projectRoot = Split-Path -Parent $scriptDir

# Try to locate a Maven wrapper or mvn on PATH. Prefer project-root mvnw.cmd -> mvnw -> global mvn
$mvnw = $null
$mvnwPath1 = Join-Path $projectRoot 'mvnw.cmd'
$mvnwPath2 = Join-Path $projectRoot 'mvnw'
$mvnwCandidates = @($mvnwPath1, $mvnwPath2, 'mvnw.cmd', 'mvnw', 'mvn')
foreach ($c in $mvnwCandidates) {
    if (Test-Path $c) {
        $mvnw = (Resolve-Path $c).Path
        break
    }
    try {
        $cmd = Get-Command $c -ErrorAction Stop
        if ($cmd -and $cmd.Path) { $mvnw = $cmd.Path; break }
    } catch {
        # ignore and continue
    }
}

Write-Host "Maven executable selected: $mvnw"
if (-not $mvnw) {
    Write-Error "Could not find mvnw/mvnw.cmd/mvn in project root or PATH. Please ensure Maven (or the Maven wrapper) is available. You can start the app manually: `mvnw spring-boot:run` or `mvn spring-boot:run` in project root. Exiting."
    exit 1
}
$python = 'python'
$venvActivate = Join-Path $projectRoot '.venv\Scripts\Activate.ps1'
$evalScript = Join-Path $projectRoot 'scripts\evaluate_sentiment.py'
$outCsv = 'evaluation_manual_label_reanalyzed.csv'
$outCsvFull = Join-Path $projectRoot $outCsv

# 1) Start app with reanalysis enabled (runs TestDataLoader and REANALYZE_SENTIMENTS)
Write-Host "Starting Spring Boot app with REANALYZE_SENTIMENTS=true... (will block this terminal)"
$env:REANALYZE_SENTIMENTS = 'true'

# Start the app (background) using Start-Process and capture the PID. Use project root as working directory.
Write-Host "Starting Spring Boot app using: $mvnw spring-boot:run ... (this will start in background)"
try {
    $proc = Start-Process -FilePath $mvnw -ArgumentList 'spring-boot:run' -WorkingDirectory $projectRoot -NoNewWindow -PassThru
    Write-Host "Started mvnw (PID=$($proc.Id)). Waiting 30 seconds for startup/seeding to begin..."
    Start-Sleep -Seconds 30
} catch {
    # Use -f formatting to avoid PowerShell interpreting `$mvnw:` inside a double-quoted string
    $errMsg = ($_ | Out-String).Trim() -replace "`r?`n"," "
    Write-Warning ("Failed to start process {0}: {1}" -f $mvnw, $errMsg)
    # Quote the path in the message so paths with spaces are clear
    Write-Host "You can start the app manually in another terminal with:`n  cd $projectRoot`n  & `"$mvnw`" spring-boot:run"
    $proc = $null
    Write-Host "Press Enter when TestDataLoader has finished (or press Ctrl+C to abort)..."
    Read-Host | Out-Null
}

Write-Host "NOTE: Tail the Maven output window to watch TestDataLoader logs. When you see 'TestDataLoader finished' and 'Re-analysis complete' you can continue."
if ($proc -ne $null) {
    Write-Host "Attempting to wait up to 300 seconds for re-analysis to complete..."
    $maxWait = 300
    $elapsed = 0
    $found = $false
    while ($elapsed -lt $maxWait) {
        Start-Sleep -Seconds 5
        $elapsed += 5
        # crude heuristic: check if the Java process is still running and give user time
        if ($proc.HasExited) { $found = $true; break }
    }

    if (-not $proc.HasExited) {
        Write-Host "Process still running. Please stop it manually (Ctrl+C) after TestDataLoader logs indicate completion, then press Enter to continue..."
        Read-Host | Out-Null
    } else {
        Write-Host "Process exited. Continuing..."
    }
} else {
    Write-Host "No process was started by this script. Assuming you started the app manually and re-analysis has completed (or you'll run it manually). Continuing..."
}

# Ensure env var cleaned
Remove-Item Env:\REANALYZE_SENTIMENTS -ErrorAction SilentlyContinue

# 2) Activate venv and run evaluation
Write-Host "Activating venv and running evaluation script..."
if (-Not (Test-Path $venvActivate)) { Write-Error "Virtualenv activate script not found at $venvActivate. Create the .venv first."; exit 1 }

# Activate current session (will only affect this script's session)
. $venvActivate

# Install deps if missing (idempotent)
python -m pip install --upgrade pip
pip install psycopg2-binary pandas scikit-learn | Out-Null

# Remove existing output file if present to avoid permission errors when writing
if (Test-Path $outCsvFull) {
    try {
        Remove-Item -Path $outCsvFull -Force -ErrorAction Stop
        Write-Host "Removed existing output file: $outCsvFull"
    } catch {
        Write-Warning "Could not remove existing output file $outCsvFull. It may be open in another application (Excel, editor) or locked. Please close it and re-run. Error: $($_.Exception.Message)"
        Write-Host "Exiting so Python can write the CSV after you free the file lock."
        exit 1
    }
}

# Run evaluation forcing manual_label (use absolute path for CSV)
python $evalScript --force-gold sentiment_analysis.manual_label --out-csv $outCsvFull

Write-Host "Evaluation complete. Output: $outCsvFull"
try {
    Start-Process $outCsvFull -ErrorAction SilentlyContinue
} catch {
    Write-Warning "Unable to open output file $outCsvFull automatically. You can open it manually."
}


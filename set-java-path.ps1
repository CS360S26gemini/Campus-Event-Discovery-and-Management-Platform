$javaHome = 'C:\Program Files\Android\Android Studio\jbr'
$javaBin = Join-Path $javaHome 'bin'
$javaExe = Join-Path $javaBin 'java.exe'

if (-not (Test-Path $javaExe)) {
    Write-Error "Java executable not found at $javaExe"
    exit 1
}

$currentJavaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
$currentUserPath = [Environment]::GetEnvironmentVariable('Path', 'User')

$pathEntries = @()
if (-not [string]::IsNullOrWhiteSpace($currentUserPath)) {
    $pathEntries = $currentUserPath -split ';' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
}

$normalizedJavaBin = $javaBin.TrimEnd('\')
$hasJavaBin = $false

foreach ($entry in $pathEntries) {
    if ($entry.TrimEnd('\') -ieq $normalizedJavaBin) {
        $hasJavaBin = $true
        break
    }
}

if (-not $hasJavaBin) {
    $pathEntries += $javaBin
}

[Environment]::SetEnvironmentVariable('JAVA_HOME', $javaHome, 'User')
[Environment]::SetEnvironmentVariable('Path', ($pathEntries -join ';'), 'User')

$pathStatus = if ($hasJavaBin) { 'already_present' } else { 'added' }

Write-Output "JAVA_HOME(User)=$javaHome"
Write-Output "JAVA_BIN_PATH_STATUS(User)=$pathStatus"
Write-Output "JAVA_EXE=$javaExe"

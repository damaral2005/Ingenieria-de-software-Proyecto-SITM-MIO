param(
    [string[]]$Hosts = @(
        "swarch@10.147.17.112",
        "swarch@10.147.17.104",
        "swarch@10.147.17.110"
    ),
    [string]$PackagePath = ".\sitm-mio-v3-ice.zip",
    [string]$RemotePackagePath = "~/sitm-mio-v3-ice.zip",
    [string]$RemoteProjectDir = "~/sitm-mio-v3"
)

$ErrorActionPreference = "Stop"

$scp = Get-Command scp -ErrorAction SilentlyContinue
if (-not $scp) {
    throw "scp was not found. Install OpenSSH Client for Windows, then run this script again."
}

$ssh = Get-Command ssh -ErrorAction SilentlyContinue
if (-not $ssh) {
    throw "ssh was not found. Install OpenSSH Client for Windows, then run this script again."
}

Write-Host "== Creating fresh deployment package =="
& "$PSScriptRoot\create-ice-deployment-package.ps1" -PackagePath $PackagePath

$absolutePackagePath = [System.IO.Path]::GetFullPath($PackagePath)
if (-not (Test-Path -LiteralPath $absolutePackagePath)) {
    throw "Package was not created: $absolutePackagePath"
}

foreach ($hostName in $Hosts) {
    Write-Host ""
    Write-Host "== Deploying to $hostName =="
    scp $absolutePackagePath "${hostName}:$RemotePackagePath"
    if ($LASTEXITCODE -ne 0) {
        throw "scp failed for $hostName with exit code $LASTEXITCODE."
    }

    $remoteCommand = "rm -rf $RemoteProjectDir && mkdir -p $RemoteProjectDir && " +
        "(unzip -oq $RemotePackagePath -d $RemoteProjectDir || " +
        "(cd $RemoteProjectDir && jar xf $RemotePackagePath)) && " +
        "test -f $RemoteProjectDir/scripts/run-ice-worker-remote.sh && " +
        "test -f $RemoteProjectDir/scripts/run-ice-master-remote.sh"

    ssh $hostName $remoteCommand
    if ($LASTEXITCODE -ne 0) {
        throw "remote extraction failed for $hostName with exit code $LASTEXITCODE."
    }

    Write-Host "Deployed clean project to ${hostName}:$RemoteProjectDir"
}

Write-Host ""
Write-Host "Deployment completed for:"
$Hosts | ForEach-Object { Write-Host "  $_" }

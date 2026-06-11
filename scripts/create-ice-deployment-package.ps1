param(
    [string]$PackagePath = ".\sitm-mio-v3-ice.zip"
)

$ErrorActionPreference = "Stop"

function ShouldNormalizeLineEndings {
    param([string]$RelativePath)

    $normalized = $RelativePath.Replace("\", "/")
    return $normalized -eq "gradlew" `
        -or $normalized.EndsWith(".sh") `
        -or $normalized.EndsWith(".md") `
        -or $normalized.EndsWith(".java") `
        -or $normalized.EndsWith(".gradle") `
        -or $normalized.EndsWith(".properties")
}

$packageDirectory = Split-Path -Parent $PackagePath
if ($packageDirectory -and -not (Test-Path -LiteralPath $packageDirectory)) {
    New-Item -ItemType Directory -Path $packageDirectory | Out-Null
}

$absolutePackagePath = [System.IO.Path]::GetFullPath($PackagePath)
$projectRoot = (Get-Location).Path
$stagingRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("sitm-mio-v3-ice-stage-" + [System.Guid]::NewGuid())

if (Test-Path -LiteralPath $absolutePackagePath) {
    Remove-Item -LiteralPath $absolutePackagePath -Force
}

$files = Get-ChildItem -LiteralPath $projectRoot -Recurse -Force -File | Where-Object {
    $relativePath = [System.IO.Path]::GetRelativePath($projectRoot, $_.FullName)
    -not (
        $relativePath -like ".git\*" -or
        $relativePath -like ".gradle\*" -or
        $relativePath -like "build\*" -or
        $relativePath -like "results\*.csv" -or
        $relativePath -like "results\*.log" -or
        $relativePath -like "*.tar.gz" -or
        $relativePath -like "*.zip" -or
        $relativePath -like "swarch@*"
    )
}

[System.Reflection.Assembly]::LoadWithPartialName("System.IO.Compression.FileSystem") | Out-Null
try {
    New-Item -ItemType Directory -Path $stagingRoot | Out-Null
    foreach ($file in $files) {
        $relativePath = [System.IO.Path]::GetRelativePath($projectRoot, $file.FullName)
        $targetPath = Join-Path $stagingRoot $relativePath
        $targetDirectory = Split-Path -Parent $targetPath
        if ($targetDirectory -and -not (Test-Path -LiteralPath $targetDirectory)) {
            New-Item -ItemType Directory -Path $targetDirectory | Out-Null
        }
        Copy-Item -LiteralPath $file.FullName -Destination $targetPath
        if (ShouldNormalizeLineEndings $relativePath) {
            $content = [System.IO.File]::ReadAllText($targetPath)
            $content = $content -replace "`r`n", "`n"
            [System.IO.File]::WriteAllText($targetPath, $content, [System.Text.UTF8Encoding]::new($false))
        }
    }

    $zipArchive = [System.IO.Compression.ZipFile]::Open(
        $absolutePackagePath,
        [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($file in Get-ChildItem -LiteralPath $stagingRoot -Recurse -Force -File) {
            $entryName = [System.IO.Path]::GetRelativePath($stagingRoot, $file.FullName).Replace("\", "/")
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $zipArchive,
            $file.FullName,
            $entryName,
            [System.IO.Compression.CompressionLevel]::Optimal
        ) | Out-Null
        }
    } finally {
        $zipArchive.Dispose()
    }
} finally {
    if (Test-Path -LiteralPath $stagingRoot) {
        Remove-Item -LiteralPath $stagingRoot -Recurse -Force
    }
}

Write-Host "Created deployment package: $absolutePackagePath"
Write-Host ""
Write-Host "Copy it to each lab PC, for example:"
Write-Host "  scp $absolutePackagePath swarch@10.147.17.104:~/"
Write-Host "  ssh swarch@10.147.17.104 'rm -rf ~/sitm-mio-v3 && mkdir -p ~/sitm-mio-v3 && unzip -oq ~/$(Split-Path -Leaf $absolutePackagePath) -d ~/sitm-mio-v3'"

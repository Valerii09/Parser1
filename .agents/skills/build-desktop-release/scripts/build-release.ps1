param(
    [string]$OutputTag = "local",
    [string]$JdkHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot",
    [string]$MavenCommand = "C:\Program Files\JetBrains\IntelliJ IDEA 2024.1.2\plugins\maven\lib\maven3\bin\mvn.cmd"
)

$ErrorActionPreference = "Stop"
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "..\..\..\.."))
$targetRoot = Join-Path $projectRoot "target"
$inputDirectory = Join-Path $targetRoot "package-input-$OutputTag"
$exeDirectory = Join-Path $targetRoot "exe-$OutputTag"
$zipPath = Join-Path $targetRoot "PaymentCourierTool-$OutputTag.zip"
$jarPath = Join-Path $targetRoot "payment-courier-tool.jar"
$jpackage = Join-Path $JdkHome "bin\jpackage.exe"

function Assert-TargetPath([string]$Path) {
    $fullPath = [System.IO.Path]::GetFullPath($Path)
    $fullTarget = [System.IO.Path]::GetFullPath($targetRoot) + [System.IO.Path]::DirectorySeparatorChar
    if (-not $fullPath.StartsWith($fullTarget, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Отказ от очистки пути вне target: $fullPath"
    }
}

if (-not (Test-Path -LiteralPath $MavenCommand)) {
    throw "Maven не найден: $MavenCommand"
}
if (-not (Test-Path -LiteralPath $jpackage)) {
    throw "jpackage не найден: $jpackage"
}

Push-Location $projectRoot
try {
    $env:JAVA_HOME = $JdkHome
    & $MavenCommand test
    if ($LASTEXITCODE -ne 0) { throw "Тесты завершились с ошибкой" }

    & $MavenCommand -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "Сборка JAR завершилась с ошибкой" }

    foreach ($path in @($inputDirectory, $exeDirectory)) {
        Assert-TargetPath $path
        if (Test-Path -LiteralPath $path) {
            Remove-Item -LiteralPath $path -Recurse -Force
        }
        New-Item -ItemType Directory -Path $path -Force | Out-Null
    }

    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $inputDirectory "payment-courier-tool.jar")

    & $jpackage `
        --type app-image `
        --dest $exeDirectory `
        --name PaymentCourierTool `
        --input $inputDirectory `
        --main-jar payment-courier-tool.jar `
        --main-class org.vc.ui.PaymentCourierToolApp `
        --app-version 1.0 `
        --java-options "-Xms512m" `
        --java-options "-XX:MaxRAMPercentage=80.0" `
        --java-options "-DpaymentCourier.fast.pdfThreads=12" `
        --java-options "-DpaymentCourier.fast.writerThreads=2"
    if ($LASTEXITCODE -ne 0) { throw "Сборка EXE завершилась с ошибкой" }

    Assert-TargetPath $zipPath
    Compress-Archive -Path (Join-Path $exeDirectory "PaymentCourierTool\*") -DestinationPath $zipPath -Force

    [PSCustomObject]@{
        Jar = $jarPath
        Exe = Join-Path $exeDirectory "PaymentCourierTool\PaymentCourierTool.exe"
        Zip = $zipPath
    }
}
finally {
    Pop-Location
}
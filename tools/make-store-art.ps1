# Generates Play Store art: 512x512 icon and 1024x500 feature graphic into store\
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
$out = Join-Path $root "store"
New-Item -ItemType Directory -Force $out | Out-Null

function New-Brush($c) { New-Object System.Drawing.SolidBrush ([System.Drawing.ColorTranslator]::FromHtml($c)) }

function Draw-Ant($g, [float]$cx, [float]$cy, [float]$s) {
    $body = New-Brush "#6D3B1F"
    $leg = New-Object System.Drawing.Pen ([System.Drawing.ColorTranslator]::FromHtml("#3E2723")), ($s * 0.07)
    $leg.StartCap = 'Round'; $leg.EndCap = 'Round'
    foreach ($side in -1, 1) {
        for ($i = 0; $i -lt 3; $i++) {
            $by = $cy + $s * (0.0 + $i * 0.14)
            $g.DrawLines($leg, [System.Drawing.PointF[]]@(
                (New-Object System.Drawing.PointF $cx, $by),
                (New-Object System.Drawing.PointF ($cx + $side * $s * 0.4), ($by - $s * 0.1)),
                (New-Object System.Drawing.PointF ($cx + $side * $s * 0.58), ($by + $s * 0.25))))
        }
    }
    $g.FillEllipse($body, $cx - $s * 0.34, $cy + $s * 0.18, $s * 0.68, $s * 0.62)
    $g.FillEllipse($body, $cx - $s * 0.2, $cy - $s * 0.1, $s * 0.4, $s * 0.36)
    $ant = New-Object System.Drawing.Pen ([System.Drawing.ColorTranslator]::FromHtml("#3E2723")), ($s * 0.05)
    $hy = $cy - $s * 0.38; $hr = $s * 0.36
    foreach ($side in -1, 1) {
        $g.DrawBezier($ant, ($cx + $side * $hr * 0.35), ($hy - $hr * 0.8), ($cx + $side * $hr * 0.5), ($hy - $hr * 1.4), ($cx + $side * $hr * 0.8), ($hy - $hr * 1.6), ($cx + $side * $hr * 1.0), ($hy - $hr * 1.65))
        $g.FillEllipse($body, ($cx + $side * $hr * 1.0 - $s * 0.07), ($hy - $hr * 1.65 - $s * 0.07), $s * 0.14, $s * 0.14)
    }
    $g.FillEllipse($body, $cx - $hr, $hy - $hr, $hr * 2, $hr * 2)
    $white = New-Brush "#FFFFFF"; $black = New-Brush "#000000"
    foreach ($side in -1, 1) {
        $ex = $cx + $side * $hr * 0.42; $ey = $hy - $hr * 0.12; $er = $hr * 0.28
        $g.FillEllipse($white, $ex - $er, $ey - $er, $er * 2, $er * 2)
        $g.FillEllipse($black, $ex - $er * 0.5 + $er * 0.15, $ey - $er * 0.5, $er, $er)
    }
    $mouth = New-Object System.Drawing.Pen ([System.Drawing.ColorTranslator]::FromHtml("#1A0E0A")), ($s * 0.05)
    $g.DrawArc($mouth, $cx - $hr * 0.32, $hy + $hr * 0.15, $hr * 0.64, $hr * 0.45, 20, 140)
}

function Draw-Bubble($g, [float]$cx, [float]$cy, [float]$r) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddEllipse($cx - $r, $cy - $r, $r * 2, $r * 2)
    $pg = New-Object System.Drawing.Drawing2D.PathGradientBrush $path
    $pg.CenterColor = [System.Drawing.Color]::FromArgb(90, 255, 255, 255)
    $pg.SurroundColors = @([System.Drawing.Color]::FromArgb(110, 129, 212, 250))
    $g.FillPath($pg, $path)
    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(230, 255, 255, 255)), ($r * 0.04)
    $g.DrawEllipse($pen, $cx - $r, $cy - $r, $r * 2, $r * 2)
    $g.FillEllipse((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(220, 255, 255, 255))), $cx - $r * 0.65, $cy - $r * 0.72, $r * 0.5, $r * 0.32)
}

function New-Canvas($w, $h) {
    $bmp = New-Object System.Drawing.Bitmap $w, $h
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'; $g.TextRenderingHint = 'AntiAliasGridFit'
    $rect = New-Object System.Drawing.Rectangle 0, 0, $w, $h
    $lg = New-Object System.Drawing.Drawing2D.LinearGradientBrush $rect, ([System.Drawing.ColorTranslator]::FromHtml("#4FC3F7")), ([System.Drawing.ColorTranslator]::FromHtml("#01579B")), 90
    $g.FillRectangle($lg, $rect)
    $wave = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(50, 255, 255, 255)), ($h / 90)
    for ($i = 1; $i -lt 6; $i++) {
        $pts = for ($x = 0; $x -le $w; $x += $w / 20) { New-Object System.Drawing.PointF $x, ($i * $h / 6 + [Math]::Sin($x / $w * 12 + $i) * $h / 40) }
        $g.DrawCurve($wave, [System.Drawing.PointF[]]$pts)
    }
    $bub = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(140, 255, 255, 255)), 3
    $rnd = New-Object System.Random 4
    for ($i = 0; $i -lt 25; $i++) { $r = $rnd.Next(4, [int]($h / 25) + 6); $g.DrawEllipse($bub, $rnd.Next(0, $w), $rnd.Next(0, $h), $r, $r) }
    return @($bmp, $g)
}

# icon 512
$c = New-Canvas 512 512; $bmp = $c[0]; $g = $c[1]
Draw-Bubble $g 256 270 200
Draw-Ant $g 256 285 150
$bmp.Save((Join-Path $out "icon-512.png"), [System.Drawing.Imaging.ImageFormat]::Png)

# feature graphic 1024x500
$c = New-Canvas 1024 500; $bmp = $c[0]; $g = $c[1]
Draw-Bubble $g 250 260 170
Draw-Ant $g 250 272 125
$font = New-Object System.Drawing.Font "Segoe UI", 96, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
$small = New-Object System.Drawing.Font "Segoe UI", 40, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
$shadow = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(110, 0, 0, 0))
$g.DrawString("Bubblant", $font, $shadow, 474, 154)
$g.DrawString("Bubblant", $font, (New-Brush "#FFFFFF"), 468, 148)
$g.DrawString("Tilt. Float. Save the ant!", $small, (New-Brush "#FFE082"), 474, 280)
$bmp.Save((Join-Path $out "feature-1024x500.png"), [System.Drawing.Imaging.ImageFormat]::Png)
Write-Host "Saved to $out"

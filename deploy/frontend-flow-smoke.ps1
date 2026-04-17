param(
    [string]$BaseUrl = "http://127.0.0.1:18080"
)

$ErrorActionPreference = "Stop"

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Uri,
        [object]$Body = $null,
        $WebSession = $null,
        [int]$TimeoutSec = 20
    )

    $params = @{
        Method = $Method
        Uri = $Uri
        TimeoutSec = $TimeoutSec
        ContentType = "application/json"
    }

    if ($null -ne $WebSession) {
        $params.WebSession = $WebSession
    }

    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 8)
    }

    try {
        return Invoke-RestMethod @params
    } catch {
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            if ($responseBody) {
                try {
                    return $responseBody | ConvertFrom-Json
                } catch {
                    throw $_
                }
            }
        }
        throw $_
    }
}

function Assert-ResultCode {
    param(
        [object]$Response,
        [int]$ExpectedCode = 200,
        [string]$Step
    )

    if ($Response.code -ne $ExpectedCode) {
        throw "$Step failed: expected code $ExpectedCode, actual $($Response.code), message=$($Response.message)"
    }
}

$ts = [DateTimeOffset]::Now.ToUnixTimeSeconds()
$authorUsername = "feauthor$ts"
$actorUsername = "feactor$ts"
$password = "pass123"
$articleTitle = "FRONT_FLOW_$ts"

$anonymousNotify = Invoke-Api -Method Get -Uri "$BaseUrl/api/notify/unread/count"

$invalidRegister = Invoke-Api -Method Post -Uri "$BaseUrl/api/user/register" -Body @{
    username = "bad$ts"
    password = "pass123"
    nickname = "BadUser"
    email = "invalid-email"
    phone = "123"
}

$authorRegister = Invoke-Api -Method Post -Uri "$BaseUrl/api/user/register" -Body @{
    username = $authorUsername
    password = $password
    nickname = "Author$ts"
    email = "$authorUsername@test.com"
    phone = "13900000001"
}
Assert-ResultCode -Response $authorRegister -Step "author register"

$actorRegister = Invoke-Api -Method Post -Uri "$BaseUrl/api/user/register" -Body @{
    username = $actorUsername
    password = $password
    nickname = "Actor$ts"
    email = "$actorUsername@test.com"
    phone = "13900000002"
}
Assert-ResultCode -Response $actorRegister -Step "actor register"

$badUserLogin = Invoke-Api -Method Post -Uri "$BaseUrl/api/user/login" -Body @{
    username = "missing_$ts"
    password = $password
}

$badPasswordLogin = Invoke-Api -Method Post -Uri "$BaseUrl/api/user/login" -Body @{
    username = $authorUsername
    password = "wrong-pass"
}

$authorSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$authorLogin = Invoke-Api -Method Post -Uri "$BaseUrl/api/user/login" -Body @{
    username = $authorUsername
    password = $password
} -WebSession $authorSession
Assert-ResultCode -Response $authorLogin -Step "author login"

$actorSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$actorLogin = Invoke-Api -Method Post -Uri "$BaseUrl/api/user/login" -Body @{
    username = $actorUsername
    password = $password
} -WebSession $actorSession
Assert-ResultCode -Response $actorLogin -Step "actor login"

$authorMe = Invoke-Api -Method Get -Uri "$BaseUrl/api/user/me" -WebSession $authorSession
Assert-ResultCode -Response $authorMe -Step "author me"

$publishResp = Invoke-Api -Method Post -Uri "$BaseUrl/api/article/publish" -Body @{
    title = $articleTitle
    summary = "frontend smoke summary"
    content = "frontend smoke content"
    boardId = 1
    tags = "smoke,frontend"
} -WebSession $authorSession
Assert-ResultCode -Response $publishResp -Step "publish article"

$articleId = $null
for ($i = 0; $i -lt 6; $i++) {
    Start-Sleep -Milliseconds 800
    $pageResp = Invoke-Api -Method Get -Uri "$BaseUrl/api/article/page/normal?pageNum=1&pageSize=50"
    Assert-ResultCode -Response $pageResp -Step "article page"
    $targetArticle = $pageResp.data.list | Where-Object { $_.title -eq $articleTitle } | Select-Object -First 1
    if ($targetArticle) {
        $articleId = $targetArticle.id
        break
    }
}

if (-not $articleId) {
    throw "published article not found in page list"
}

$detailBefore = Invoke-Api -Method Get -Uri "$BaseUrl/api/article/detail/$articleId" -WebSession $actorSession
Assert-ResultCode -Response $detailBefore -Step "detail before interaction"

$commentResp = Invoke-Api -Method Post -Uri "$BaseUrl/api/comment" -Body @{
    articleId = $articleId
    content = "frontend smoke comment"
} -WebSession $actorSession
Assert-ResultCode -Response $commentResp -Step "comment create"

Start-Sleep -Seconds 2

$notifyCount = Invoke-Api -Method Get -Uri "$BaseUrl/api/notify/unread/count" -WebSession $authorSession
Assert-ResultCode -Response $notifyCount -Step "notify unread count"

$notifyPage = Invoke-Api -Method Post -Uri "$BaseUrl/api/notify/page" -Body @{
    pageNum = 1
    pageSize = 10
} -WebSession $authorSession
Assert-ResultCode -Response $notifyPage -Step "notify page"

$profileUpdate = Invoke-Api -Method Put -Uri "$BaseUrl/api/user/info" -Body @{
    nickname = "Updated$ts"
    avatar = "https://example.com/avatar-$ts.png"
    email = "updated$ts@test.com"
    phone = "13900000003"
} -WebSession $authorSession
Assert-ResultCode -Response $profileUpdate -Step "profile update"

$profileAfter = Invoke-Api -Method Get -Uri "$BaseUrl/api/user/me" -WebSession $authorSession
Assert-ResultCode -Response $profileAfter -Step "profile reload"

$likeResp = Invoke-Api -Method Put -Uri "$BaseUrl/api/article/like/$articleId" -WebSession $actorSession
Assert-ResultCode -Response $likeResp -Step "like article"
$likedState = Invoke-Api -Method Get -Uri "$BaseUrl/api/article/liked/$articleId" -WebSession $actorSession
Assert-ResultCode -Response $likedState -Step "liked state"

$favoriteResp = Invoke-Api -Method Put -Uri "$BaseUrl/api/article/favorite/$articleId" -WebSession $actorSession
Assert-ResultCode -Response $favoriteResp -Step "favorite article"
$favoritedState = Invoke-Api -Method Get -Uri "$BaseUrl/api/article/favorited/$articleId" -WebSession $actorSession
Assert-ResultCode -Response $favoritedState -Step "favorited state"

$detailAfterOn = Invoke-Api -Method Get -Uri "$BaseUrl/api/article/detail/$articleId" -WebSession $actorSession
Assert-ResultCode -Response $detailAfterOn -Step "detail after like/favorite"

$unlikeResp = Invoke-Api -Method Delete -Uri "$BaseUrl/api/article/like/$articleId" -WebSession $actorSession
Assert-ResultCode -Response $unlikeResp -Step "unlike article"
$likedOffState = Invoke-Api -Method Get -Uri "$BaseUrl/api/article/liked/$articleId" -WebSession $actorSession
Assert-ResultCode -Response $likedOffState -Step "liked state off"

$unfavoriteResp = Invoke-Api -Method Delete -Uri "$BaseUrl/api/article/favorite/$articleId" -WebSession $actorSession
Assert-ResultCode -Response $unfavoriteResp -Step "unfavorite article"
$favoritedOffState = Invoke-Api -Method Get -Uri "$BaseUrl/api/article/favorited/$articleId" -WebSession $actorSession
Assert-ResultCode -Response $favoritedOffState -Step "favorited state off"

$detailAfterOff = Invoke-Api -Method Get -Uri "$BaseUrl/api/article/detail/$articleId" -WebSession $actorSession
Assert-ResultCode -Response $detailAfterOff -Step "detail after unlike/unfavorite"

$result = [ordered]@{
    anonymous_notify_code = $anonymousNotify.code
    anonymous_notify_message = $anonymousNotify.message
    invalid_register_code = $invalidRegister.code
    invalid_register_message = $invalidRegister.message
    bad_user_login_code = $badUserLogin.code
    bad_user_login_message = $badUserLogin.message
    bad_password_login_code = $badPasswordLogin.code
    bad_password_login_message = $badPasswordLogin.message
    author_login_code = $authorLogin.code
    actor_login_code = $actorLogin.code
    article_id = $articleId
    notify_unread = $notifyCount.data
    notify_first_title = @($notifyPage.data.list | Select-Object -ExpandProperty title)[0]
    profile_after_nickname = $profileAfter.data.nickname
    profile_after_email = $profileAfter.data.email
    liked_state_after_on = $likedState.data
    favorited_state_after_on = $favoritedState.data
    like_count_after_on = $detailAfterOn.data.likeCount
    favorite_count_after_on = $detailAfterOn.data.favoriteCount
    liked_state_after_off = $likedOffState.data
    favorited_state_after_off = $favoritedOffState.data
    like_count_after_off = $detailAfterOff.data.likeCount
    favorite_count_after_off = $detailAfterOff.data.favoriteCount
}

$result | ConvertTo-Json -Depth 6

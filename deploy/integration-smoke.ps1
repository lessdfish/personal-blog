param(
    [string]$BaseUrl = "http://127.0.0.1:18080"
)

$ErrorActionPreference = "Stop"

$ts = [DateTimeOffset]::Now.ToUnixTimeSeconds()
$authorName = "sa$ts"
$replyName = "sr$ts"
$password = "pass123"

function Invoke-JsonPost {
    param(
        [string]$Uri,
        [hashtable]$Body,
        [hashtable]$Headers = @{}
    )
    return Invoke-RestMethod -Uri $Uri -Method Post -ContentType "application/json" -Headers $Headers -Body ($Body | ConvertTo-Json) -TimeoutSec 20
}

$null = Invoke-JsonPost -Uri "$BaseUrl/api/user/register" -Body @{
    username = $authorName
    password = $password
    nickname = "SmokeAuthor"
    email = "$authorName@test.com"
    phone = "13900000021"
}

$null = Invoke-JsonPost -Uri "$BaseUrl/api/user/register" -Body @{
    username = $replyName
    password = $password
    nickname = "SmokeReply"
    email = "$replyName@test.com"
    phone = "13900000022"
}

$authorLogin = Invoke-JsonPost -Uri "$BaseUrl/api/user/login" -Body @{
    username = $authorName
    password = $password
}
$replyLogin = Invoke-JsonPost -Uri "$BaseUrl/api/user/login" -Body @{
    username = $replyName
    password = $password
}

$authorToken = $authorLogin.data.token
$replyToken = $replyLogin.data.token
$authorHeaders = @{ Authorization = "Bearer $authorToken" }
$replyHeaders = @{ Authorization = "Bearer $replyToken" }

$publishResp = Invoke-JsonPost -Uri "$BaseUrl/api/article/publish" -Headers $authorHeaders -Body @{
    title = "SMOKE_ARTICLE_$ts"
    summary = "smoke summary"
    content = "smoke content"
    boardId = 1
    tags = "smoke,test"
}

$article = $null
for ($i = 0; $i -lt 5; $i++) {
    Start-Sleep -Seconds 1
    $pageResp = Invoke-RestMethod -Uri "$BaseUrl/api/article/page/normal?pageNum=1&pageSize=50" -TimeoutSec 20
    $article = $pageResp.data.list | Where-Object { $_.title -eq "SMOKE_ARTICLE_$ts" } | Select-Object -First 1
    if ($article) {
        break
    }
}
if (-not $article) {
    throw "Article not found after publish"
}

$detailResp = Invoke-RestMethod -Uri "$BaseUrl/api/article/detail/$($article.id)" -TimeoutSec 20
$commentResp = Invoke-JsonPost -Uri "$BaseUrl/api/comment" -Headers $replyHeaders -Body @{
    articleId = $article.id
    content = "smoke comment"
}

Start-Sleep -Seconds 2

$notifyCount = Invoke-RestMethod -Uri "$BaseUrl/api/notify/unread/count" -Headers $authorHeaders -TimeoutSec 20
$notifyPage = Invoke-JsonPost -Uri "$BaseUrl/api/notify/page" -Headers $authorHeaders -Body @{
    pageNum = 1
    pageSize = 10
}

$logoutResp = Invoke-RestMethod -Uri "$BaseUrl/api/user/logout" -Method Post -Headers $replyHeaders -TimeoutSec 20
$oldTokenResp = Invoke-RestMethod -Uri "$BaseUrl/api/notify/unread/count" -Headers $replyHeaders -TimeoutSec 20

$result = [ordered]@{
    register_author = "OK"
    register_reply = "OK"
    login_author_code = $authorLogin.code
    login_reply_code = $replyLogin.code
    publish_code = $publishResp.code
    article_id = $article.id
    detail_code = $detailResp.code
    comment_code = $commentResp.code
    comment_id = $commentResp.data
    notify_count_code = $notifyCount.code
    notify_count = $notifyCount.data
    notify_page_code = $notifyPage.code
    notify_titles = @($notifyPage.data.list | Select-Object -ExpandProperty title)
    logout_code = $logoutResp.code
    old_token_code = $oldTokenResp.code
    old_token_message = $oldTokenResp.message
}

$result | ConvertTo-Json -Depth 6

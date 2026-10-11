// 現在の requestJson() の !response.ok 部分で message を優先表示する例。
// app.js全体をこのファイルで置換しないでください。
if (!response.ok) {
    throw new Error(
        data?.message || '処理を完了できませんでした。'
    );
}

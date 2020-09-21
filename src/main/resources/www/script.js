var webSocket = new WebSocket("ws://rps.stnwtr.de/ws");
//var webSocket = new WebSocket("ws://10.20.128.69:7435/ws");
var loggedIn = false;
webSocket.onopen = login;
webSocket.onerror = (e) => console.log(e);

var latestStats = {};

$( document ).ready(function() {
    $(".header").hide();
});

function handleMessage(messageEvent) {
    console.log("Received Message: " + messageEvent.data)
    let parsedData = JSON.parse(messageEvent.data);

    switch (parsedData.type) {
        case "login":
            Cookies.set('loginID', parsedData.cookie);
            Cookies.set('username', parsedData.username);
            loggedIn = true;
            break;
        case "stats":
            latestStats = parsedData;

            //TODO: Set Stats!
            break;

        case "start-game":
            //TODO: set Stats
            $(".btn").html("Play");
            $("#login").fadeOut();
            $("#foreignRank").val("Rank: "+parsedData.enemyRank);
            $("#foreignName").val(parsedData.enemyName);
            $("#ownRank").val("Rank: "+ parsedData.ownRank);
            $(".header").fadeIn();


            break;
        case "move-result":
            $("#ownScore").val(parsedData.ownPoints);
            $("#foreignScore").val(parsedData.enemyPoints);
            break;
        case "finish":
            //TODO: process game result
            break;

    }
}

webSocket.onmessage = handleMessage;

function login() {
    function loginWithCookie() {
        $("#overview").show();
        console.log("Logging in with the Username:" + $("#name").val())

        webSocket.send(JSON.stringify(
            {
                "type": "login",
                "username": $("#name").val(),
                "cookie": Cookies.get('loginID')
            }
        ));
        setTimeout(() => {
            webSocket.send(JSON.stringify({"type": "stats"}));
        }, 500);
    }

    if ('loginID' in Cookies.get())
        loginWithCookie();
    else{
        $("#overview").hide();
        $("#login").show();
    }
}

function startGame(){
    $(".btn").html("Searching...");
    if (!loggedIn) {
        webSocket.send(JSON.stringify(
            {
                "type": "login",
                "username": $("#name").val(),
                "cookie": null
            }
        ));
    }

    webSocket.send(JSON.stringify({"type": "start-game"}));
}

function startGameInitial() {
    if($("#name").val().length===0){
        alert("Please specify a name first!");
        return;
    }
    startGame();
}

function chooseMove(name){
    console.log("Choosing "+name)
    webSocket.send(JSON.stringify({"type":"selection", "value":name}));
}
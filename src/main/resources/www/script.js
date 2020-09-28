var webSocket = new WebSocket("ws://rps.stnwtr.de/ws");
//var webSocket = new WebSocket("ws://10.20.128.69:7435/ws");
var loggedIn = false;
webSocket.onopen = login;
webSocket.onerror = (e) => console.log(e);

var latestStats = {};

$(document).ready(function () {
    $(".header").hide();
});

function handleMessage(messageEvent) {
    console.log("Received Message: " + messageEvent.data)
    let parsedData = JSON.parse(messageEvent.data);

    switch (parsedData.type) {
        case "login":
            Cookies.set('loginID', parsedData.uuid);
            Cookies.set('username', parsedData.username);
            $("#helloUsername").text("Hello " + parsedData.username);
            loggedIn = true;
            break;
        case "stats":
            latestStats = parsedData;
            $("#numberOfWins").text(parsedData.wins);
            $("#numberOfDefeats").text(parsedData.defeats);
            $("#skillRating").text(parsedData.score);
            break;

        case "start-game":
            $(".btn").html("Play");
            $("#login").fadeOut();
            $("#foreignRank").text("Rank: " + parsedData.enemyRank);
            $("#foreignName").text(parsedData.enemyName);
            $("#ownRank").text("Rank: " + parsedData.ownRank);
            $("#ownName").text(Cookies.get("username"));
            $(".header").fadeIn();
            $("#selectRPS").show();
            $("#overview").hide();
            $("#opponentMove").show();

            break;
        case "move-result":
            $("#ownScore").text(parsedData.ownPoints);
            $("#foreignScore").text(parsedData.enemyPoints);
            $("#my_image").attr("src", "second.jpg");

            $("#opponentMove").attr('src', 'img/hand-' + parsedData.enemyMove + '.png');


            setTimeout(() => {
                $("#opponentMove").attr('src', 'img/questionMark.png');

                $("#activeSelection").fadeOut(() => {
                    $("#selectRPS").fadeIn();
                });

            }, 1000);
            break;
        case "finish":
            $("#resultBox").fadeIn();

            if (parsedData.result === "win")
                $("#winMessage").fadeIn();
            else
                $('#defeatMessage').fadeIn();

            setTimeout(() => {
                location.reload();
            }, 3000);
            break;
    }
}

webSocket.onmessage = handleMessage;

function login() {
    function loginWithCookie() {
        $("#overview").show();
        console.log("Logging in with the Username:" + Cookies.get('username'))

        loggedIn = true;
        webSocket.send(JSON.stringify(
            {
                "type": "login",
                "username": Cookies.get('username'),
                "uuid": Cookies.get('loginID')
            }
        ));
        console.log("Requested stats");
        webSocket.send(JSON.stringify({"type": "stats"}));
    }

    if ('loginID' in Cookies.get())
        loginWithCookie();
    else {
        $("#overview").hide();
        $("#login").show();
    }
}

function startGame() {
    $(".btn").html("Searching...");
    if (!loggedIn) {
        webSocket.send(JSON.stringify(
            {
                "type": "login",
                "username": $("#name").val(),
                "uuid": null
            }
        ));
    }

    webSocket.send(JSON.stringify({"type": "start-game"}));
}

function startGameInitial() {
    if ($("#name").val().length === 0) {
        alert("Please specify a name first!");
        return;
    }
    startGame();
}

function chooseMove(name) {
    console.log("Choosing " + name);
    webSocket.send(JSON.stringify({"type": "selection", "value": name}));
    $("#selectRPS").fadeOut(() => {
        $("#activeSelection").fadeIn().attr('src', 'img/hand-' + name + '.png');
    });

}
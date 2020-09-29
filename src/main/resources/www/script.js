var webSocket = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws")
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
            Cookies.set('loginID', parsedData.uuid, {expires: 365*10});
            Cookies.set('username', parsedData.username, {expires: 365*10});
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

            setTimeout(()=>
            {
                $("#opponentMove").attr('src', 'img/hand-' + parsedData.enemyMove + '.png');
            }, 800);


            setTimeout(() => {
                $("#opponentMove").attr('src', 'img/questionMark.png');

                $("#activeSelection").fadeOut(() => {
                    $("#selectRPS").show();
                });

            }, 2500);
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
    $("#selectRPS").fadeOut("fast", () => {
        $("#activeSelection").fadeIn("fast").attr('src', 'img/hand-' + name + '.png');
    });

}

var theme = new Audio("RRPS.mp3");
theme.muted = true;
theme.loop = true;

function mute() {
    theme.play()
    if (theme.muted === true) {
        $("#vol_off").hide();
        $("#vol_on").show();
        theme.muted = false;
    }
    else{
        $("#vol_off").show();
        $("#vol_on").hide();
        theme.muted = true;
    }

}
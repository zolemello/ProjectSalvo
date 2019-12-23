// VUE 
var app = new Vue({
el: "#app",
data: {
games: [],
current_player: [],
playersLeaderBoard: []
}
});


$(document).ready(function() {
getGames();
});

// GET GAMES 
function getGames(){
$.get("/api/games").done(function(result) {
app.games=result.games;
console.log("current player: ",result.players);
app.current_player = result.players;
dateTransform(app.games);
playersArray = getPlayers(app.games);
addScoresToPlayersArray(playersArray,app.games);
registerForm(result.players);
});
}

//LOG IN
$("#botonLogin").click(function() {
if (!$("#inputUserName").val() || !$("#inputPassword").val()) {
$("#login-alert").html("Please enter email and password.");
} else if (!correctEmailFormat($("#inputUserName").val())) {
$("#login-alert").html("Please enter valid email format.");
} else {
$.post("/api/login", { username: $("#inputUserName").val(), password: $("#inputPassword").val() })
.done(function() {
$("#login-alert").html("logged in");
getGames();
})
.fail(function() {
$("#login-alert").html("Invalid User or Password. Please try again.");
});
}
});

//LOG OUT 
$("#botonLogout").click(function() {
$.post("/api/logout").done(function() {
getGames();
console.log("logged out");
})
});

// SINGUP
$("#botonSingup").click(function() {
if (!$("#inputEmail").val() || !$("#signPassword").val()) {
$("#signUp-alert").html("Please complete form to Sign Up.");
} else if (!correctEmailFormat($("#inputEmail").val())) {
$("#signUp-alert").html("Please enter valid email format.");
} else {
$.post("/api/players", { username: $("#inputEmail").val(), password: $("#signPassword").val() })
.done(function() {
$.post("/api/login", { username: $("#inputEmail").val(), password: $("#signPassword").val() })
.done(function() {
getGames();
$("#signUp-form").hide();
})
.fail(function() {
$("#signUp-alert").html("Login error. Please try again");
})
})
.fail(function() {
$("#signUp-alert").html("User already exist");
})
}
});

function correctEmailFormat(email){
var RegExp = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/

return RegExp.test(email);
}

$("#go-to-botonSingup").click(function() {
$("#login-alert").html("");
$("#signUp-alert").html("");
$("#login-form").hide();
$("#signUp-form").show();
});

$("#back-to-login").click(function() {
$("#login-alert").html("");
$("#signUp-alert").html("");
$("#signUp-form").hide();
$("#login-form").show();
});


function registerForm(player) {
if (player == null) {

$("#login-form").show();
$("#logout-form").hide();
} else {
$("#login-form").hide();
$("#logout-form").show();
}
}

//FECHA
function dateTransform(array){
for (var i=0;i<app.games.length;i++) {
var newDate = new Date(app.games[i].created).toLocaleString();
app.games[i].created = newDate;
}
}

//LEADERBOARD 
function addScoresToPlayersArray(players, games) {

for (let i = 0; i < games.length; i++) {

for (let j = 0; j < games[i].scores.length; j++) {

if(games[i].scores[j]  != null){

let scorePlayerId = games[i].scores[j].player;

for (let k = 0; k < players.length; k++) {

if (players[k].id == scorePlayerId) {
players[k].scores.push(games[i].scores[j].score);
players[k].total += games[i].scores[j].score;
}
}
}

}


}
showScoreBoard(players);
app.playersLeaderBoard = players;
}

function showScoreBoard(players) {

players.sort(function (a, b) {
return b.total - a.total;
});


for (let i = 0; i < players.length; i++) {
let cantWon= 0;
let cantLost = 0;
let cantTied = 0;

if (players[i].scores.length > 0) {

for (let j = 0; j < players[i].scores.length; j++) {
if (players[i].scores[j] == 0.0) {
cantLost++;
} else if (players[i].scores[j] == 0.5) {
cantTied++;
} else if (players[i].scores[j] == 1.0) {
cantWon++;
}
}
players[i].lost=cantLost;
players[i].tied=cantTied;
players[i].win=cantWon;
}
}
}

function getPlayers(data) {

let players = [];
let playersIds = [];

for (let i = 0; i < data.length; i++) {

for (let j = 0; j < data[i].players.length; j++) {

if (!playersIds.includes(data[i].players[j].id)) {
playersIds.push(data[i].players[j].id);
let playerScoreData = {
"id": data[i].players[j].id,
"email": data[i].players[j].email,
"scores": [],
"total": 0.0,
"win":0.0,
"lost":0.0,
"tied":0.0
};
players.push(playerScoreData);
}
}
}
return players;
}

// CREA UN JUEGO

$("#app").on("click", "#new-game-btn", function() {
$.post("/api/games").done(function(response) {
$("#fail-creation-game-alert").html("");
location.assign("/web/game.html?gp="+response.gpId);
}).fail(function() {
$("#fail-creation-game-alert").html("Something went wrong. Please try again later");
})
});


// SE UNE A UN JUEGO

$("#app").on("click", ".join-game-btn", function() {
var gameId = $(this).data("game");
$.post("/api/game/"+gameId+"/players").done(function(response) {
$(".fail-joining-game-alert").html("");
location.assign("/web/game.html?gp="+response.gpId);
}).fail(function() {
$(".fail-joining-game-alert").html("");
$(".fail-joining-game-alert").filter("[data-game="+gameId+"]").html("Game is full");
})
});


// ENTRA A UN JUEGO SUYO YA CREADO 
$("#app").on("click", ".rejoin-game-btn", function() {
var gamePlayerId = $(this).data("game");
location.assign("/web/game.html?gp="+gamePlayerId);
});
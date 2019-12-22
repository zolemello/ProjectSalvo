//--------------------------------VUE TEMPLATE----------------------------------
var app = new Vue({
el: "#app",
data: {
games: [],
current_player: [],
playersLeaderBoard: []
}
});

//--------------------------------GET INIT DATA----------------------------------
$(document).ready(function() {
getData();
});

function getData(){
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

//--------------------------------REGISTRATION FORM----------------------------------
$("#login-btn").click(function() {
if (!$("#inputUserName").val() || !$("#inputPassword").val()) {
$("#login-alert").html("Please enter email and password.");
} else if (!correctEmailFormat($("#inputUserName").val())) {
$("#login-alert").html("Please enter valid email format.");
} else {
$.post("/api/login", { username: $("#inputUserName").val(), password: $("#inputPassword").val() })
.done(function() {
$("#login-alert").html("logged in");
getData();
})
.fail(function() {
$("#login-alert").html("Invalid User or Password. Please try again.");
});
}
});

$("#logout-btn").click(function() {
$.post("/api/logout").done(function() {
getData();
console.log("logged out");
})
});

$("#sign-up-btn").click(function() {
if (!$("#inputEmail").val() || !$("#signPassword").val()) {
$("#signUp-alert").html("Please complete form to Sign Up.");
} else if (!correctEmailFormat($("#inputEmail").val())) {
$("#signUp-alert").html("Please enter valid email format.");
} else {
$.post("/api/players", { username: $("#inputEmail").val(), password: $("#signPassword").val() })
.done(function() {
$.post("/api/login", { username: $("#inputEmail").val(), password: $("#signPassword").val() })
.done(function() {
$("#signUp-alert").html("");
getData();
$("#signUp-form").hide();
})
.fail(function() {
$("#signUp-alert").html("Login error. Please try again");
})
})
.fail(function() {
$("#signUp-alert").html("This user already exist");
})
}
});

function correctEmailFormat(email){
var RegExp = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/

return RegExp.test(email);
}

$("#go-to-sign-up-btn").click(function() {
$("#login-alert").html("");
$("#signUp-alert").html("");
$("#login-form").hide();
$("#signUp-form").show();
});

$("#back-to-login-btn").click(function() {
$("#login-alert").html("");
$("#signUp-alert").html("");
$("#signUp-form").hide();
$("#login-form").show();
});


function registerForm(player) {
if (player == null) {
$("#profile-title").html("Registration");
$("#login-form").show();
$("#logout-form").hide();
} else {
$("#logged-in-name").html("Welcome, "+player.email);
$("#profile-title").html("My Profile");
$("#login-form").hide();
$("#logout-form").show();
}
}

//---------------------------------------DATE FORMAT---------------------------------------
function dateTransform(array){
for (var i=0;i<app.games.length;i++) {
var newDate = new Date(app.games[i].created).toLocaleString();
app.games[i].created = newDate;
}
}

//--------------------------------LEADER BOARD STATISTICS----------------------------------
function addScoresToPlayersArray(players, games) {

for (let i = 0; i < games.length; i++) {

for (let j = 0; j < games[i].scores.length; j++) {
console.log(games[i].scores[j]);
if(games[i].scores[j]  != null){
console.log("Entre al if");
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


for (let m = 0; m < players.length; m++) {
let countWon = 0;
let countLost = 0;
let countTied = 0;

if (players[m].scores.length > 0) {

for (let n = 0; n < players[m].scores.length; n++) {
if (players[m].scores[n] == 0.0) {
countLost++;
} else if (players[m].scores[n] == 0.5) {
countTied++;
} else if (players[m].scores[n] == 1.0) {
countWon++;
}
}
players[m].lost=countLost;
players[m].tied=countTied;
players[m].win=countWon;
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

//--------------------------------CREATE AND JOIN GAMES----------------------------------

$("#app").on("click", "#new-game-btn", function() {
$.post("/api/games").done(function(response) {
$("#fail-creation-game-alert").html("");
location.assign("/web/game.html?gp="+response.gpid);
}).fail(function() {
$("#fail-creation-game-alert").html("Something went wrong. Please try again later");
})
});


$("#app").on("click", ".join-game-btn", function() {
var gameId = $(this).data("game");
$.post("/api/game/"+gameId+"/players").done(function(response) {
console.log("Realicé el post");
$(".fail-joining-game-alert").html("");
location.assign("/web/game.html?gp="+response.gpid);
}).fail(function() {
$(".fail-joining-game-alert").html("");
$(".fail-joining-game-alert").filter("[data-game="+gameId+"]").html("Game is full");
})
});

$("#app").on("click", ".rejoin-game-btn", function() {
//chequeo que pase bien el gpid
console.log($(this).data("game"));
//armo la url
var gamePlayerId = $(this).data("game");
location.assign("/web/game.html?gp="+gamePlayerId);
});
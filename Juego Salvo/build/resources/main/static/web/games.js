//VUE
var app = new Vue({
    el: "#app",
    data: {
        games: [],
        leaderboard:[],
        player: []
    }
});

$(document).ready(function(){
    getGames()
});


function changeDateFormat (){
    for (i in app.games){
        var newDate = new Date(app.games[i].created).toLocaleString();
        app.games[i].created = newDate
    }
}

//OBTIENE LOS JUEGOS

function getGames(){
    $.get("/api/games").done(function(result){
        app.games=result.games;
        console.log("current player:", result.player);
        app.player=result.player;
        changeDateFormat();
        getLeaders();
    })

}

// PARA EL LEADERBOARD
function getLeaders(){
    fetch("/api/leaderboard")
    .then(res => res.json())
    .then(json => {
    console.log(json);
        app.leaderboard = json
    })
}

//LOGIN
$("#loginbtn").click(function(){
    console.log($("#inputUserName").val());
	if (!$("#inputUserName").val() || !$("#inputPassword").val()) {
        $("#login-alert").html("Please enter a valid email and password.");
		} else {
    $.post("/api/login",{ username:$("#inputUserName").val(),password:$("#inputPassword").val()})
    .done(function(){
        getGames();
        console.log("LOGGED IN!");
    })
    .fail(function(){
        console.log("LOGIN FAILED");
    })
	
	}
});


//LOGOUT

$("#logoutbtn").click(function() {
$.post("/api/logout").done(function() {
getGames();
console.log("Logged out");

})

});


//SINGUP

$("#signupbtn").click(function() {
    
        $.post("/api/players", { username: $("#inputUserName").val(), password: $("#inputPassword").val() })
            .done(function() {
                $.post("/api/login", { username: $("#inputUserName").val(), password: $("#inputPassword").val() })
                    .done(function() {
                       
                        getGames();
                        console.log("Signed up"); 
                    })
                    .fail(function() {
                        console.log("Error. Please try again");
                    })
           
    }
)};


//JOIN GAME
function joinGame(gameId){
    		$.post("/api/games/" + gameId + "/players")
			.done(function(){
   			
    			location.href = "/web/game.html?gp=" + json.gpId
    		
			.fail(function() {
    		.catch(error => error)
    		.then(error => console.log(error))
    	} )
})};
		
		
//CREATE GAME
function createGame(){
       $.post("/api/games/")
	   .done(function() {
		                  
              (  location.href = '/web/game.html?gp=' + json.gpId
            )
            .catch(error => error)
            .then(error => console.log(error))
        }
	)
	
};

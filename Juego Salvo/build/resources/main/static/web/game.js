//-------------------------------------------------------VUE FRAMEWORK---------------------------------------------------------
var app = new Vue({
  el: "#app",
  data: {
    grid: {
            "numbers": ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"],
            "letters": ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"]
    },
    player_1: "",
    player_2: "",
    gameViewerSide: "",
    gameViewerShips: [],
    allShipPositions: [],
    viewerPlayerId: 0,
    viewerSalvoTurn: "",
    viewerGameState:""
  }
});

//-------------------------------------------------------------WHEN DOM READY, CALL LOAD DATA---------------------------------------------------------
$(function() {
    refreshData();
    loadData();
});

//-------------------------------------------------------------------REFRESH DATA WHEN WAITING OPPONENT MOVE---------------------------------------------------------
var timerId;

function refreshData() {
    timerId = setInterval(function() { loadData(); }, 10000);
}

function stopRefreshing() {
    clearInterval(timerId);
}

//-------------------------------------------------------MAIN FUNCTION WHEN PAGE LOADS - AJAX GET DATA---------------------------------------------------------
function loadData() {
    var gamePlayerId = getQueryVariable("gp");
    console.log("GamePlayerID="+gamePlayerId);
    $.get("/api/game_view/"+gamePlayerId).done(function(gameDTO) {
    showPlayersByGamePlayerId(gamePlayerId, gameDTO);

        $("#audio-theme").html('<source src="css/audio/light-side-theme.mp3" type="audio/mp3">');

        if (gameDTO.ships.length === 0) {
            stopRefreshing();
            placeNewShips();
        } else {
            var grid = $('#grid').data('gridstack');
            if ( typeof grid != 'undefined' ) {
                grid.removeAll();
                grid.destroy(false);
            }
            app.gameViewerShips = gameDTO.ships;
            getAllShipLocations(app.gameViewerShips);
            placeShipsFromBackEnd();
            if ($("#salvo-col").hasClass("display-none")) {
                $("#salvo-col").removeClass("display-none");
                $("#salvo-col").addClass("display-block");
            }
            getCurrentTurn(gameDTO.salvoes);
            displaySalvoes(gamePlayerId, gameDTO);
        }

        if (app.viewerGameState === "WIN" || app.viewerGameState === "LOSE" || app.viewerGameState === "DRAW") {
            app.viewerSalvoTurn="GAME ENDED: YOU "+app.viewerGameState+"!"
            $("#fire-salvo-btn").hide();
            stopRefreshing();
        }
    })
    .fail(function () {
        console.log("Failed to get game view data... ");
    });
}

//-------------------------------------------------------SHOW PLAYERS NAMES---------------------------------------------------------
function showPlayersByGamePlayerId(id, obj) {

    obj.gamePlayers.map(function (gamePlayer) {
        if (id == gamePlayer.gpid) {
            app.player_1 = gamePlayer.email + " (you)";
            app.viewerPlayerId = gamePlayer.id;
            app.viewerGameState = gamePlayer.gameState;
        } else if (id != gamePlayer.gpid) {
            app.player_2 = " vs " + gamePlayer.email;
        }
    });
}

//-------------------------------------------------------LOGOUT FUNCTION---------------------------------------------------------
$("#logout").click(function() {
    $.post("/api/logout")
    .done(function() {
        window.location.replace("/web/games2.html");
    })
    .fail(function () {
        console.log("Error");
    })
});

//-------------------------------------------------------GET QUERY PARAM FUNCTION---------------------------------------------------------
function getQueryVariable(variable) {
   var query = window.location.search.substring(1);
   var vars = query.split("&");
   for (var i=0;i<vars.length;i++) {
       var pair = vars[i].split("=");
       if(pair[0] == variable){
           return pair[1];
       }
   }
   return(false);
}

//-------------------------------------------------------DISPLAY SALVOS FUNCTION---------------------------------------------------------
function displaySalvoes(gamePlayerId, gameDTO) {

   for (var i=0;i<gameDTO.gamePlayers.length;i++){

       if (gameDTO.gamePlayers[i].gpid == gamePlayerId) {
           var thisPlayerId = gameDTO.gamePlayers[i].id;
           gameDTO.salvoes.map(function (salvo) {
               if (salvo.player == thisPlayerId) {
                   var myTurn = salvo.turn;
                   for (var e=0;e<salvo.locations.length;e++){
                       var letterP1 = salvo.locations[e].substring(0, 1);
                       var numberP1 = salvo.locations[e].substring(1, 3);
                       $("#salvo-body>."+letterP1+" td:eq("+numberP1+")").addClass("bg-salvo").html(myTurn);
                   }
               } else if (salvo.player != thisPlayerId) {
                   var yourTurn = salvo.turn;
                   for (var h=0;h<salvo.locations.length;h++){
                       var letter = salvo.locations[h].substring(0, 1);
                       var number = salvo.locations[h].substring(1, 3);
                       if ($("#grid-body>."+letter+" td:eq("+number+")").hasClass("bg-ship")) {
                           $("#grid-body>."+letter+" td:eq("+number+")").addClass("bg-salvo").html(yourTurn);
                       }
                   }
               }
           });
       }
   }
}


//----------------------------------------------------------------AJAX POST NEW SHIPS-----------------------------------------------------------------
function postShips(shipTypeAndCells) {
    var gamePlayerId = getQueryVariable("gp");
    $.post({
      url: "/api/games/players/"+gamePlayerId+"/ships",
      data: JSON.stringify(shipTypeAndCells),
      dataType: "text",
      contentType: "application/json"
    })
    .done(function (response) {
      refreshData();
      loadData();
      console.log( "Ships added: " + response );
    })
    .fail(function () {
      console.log("Failed to add ships... ");
    })
}

//-------------------------------------------------------ON CLICK BATTLE - POST NEW SHIPS---------------------------------------------------------
$("#placed-ships-btn").click(function(){
    var shipTypeAndCells = [];

    for (var i=1; i<=5; i++) {
        var ship = new Object();
        var cellsArray = [];

        var h = parseInt($("#grid .grid-stack-item:nth-child("+i+")").attr("data-gs-height"));
        var w = parseInt($("#grid .grid-stack-item:nth-child("+i+")").attr("data-gs-width"));
        var posX = parseInt($("#grid .grid-stack-item:nth-child("+i+")").attr("data-gs-x"));
        var posY = parseInt($("#grid .grid-stack-item:nth-child("+i+")").attr("data-gs-y"))+64;

        if (w>h) {
            for (var e=1; e<=w; e++) {
                var HHH = String.fromCharCode(posY+1)+(posX+e);
                cellsArray.push(HHH);
                ship.type = $("#grid .grid-stack-item:nth-child("+i+")").children().attr("alt");
                ship.shipLocations = cellsArray;
            }
        } else if (h>w) {
            for (var d=1; d<=h; d++) {
                var VVV = String.fromCharCode(posY+d)+(posX+1);
                cellsArray.push(VVV);
                ship.type = $("#grid .grid-stack-item:nth-child("+i+")").children().attr("alt");
                ship.shipLocations = cellsArray;
            }
        }
        console.log(ship.type,ship.shipLocations);
        shipTypeAndCells.push(ship);
    }
    postShips(shipTypeAndCells);
})

//-------------------------------------------------------ROTATE SHIPS EVENT---------------------------------------------------------
function setListener(grid) {
    $(".grid-stack-item").dblclick(function() {
        var h = parseInt($(this).attr("data-gs-height"));
        var w = parseInt($(this).attr("data-gs-width"));
        var posX = parseInt($(this).attr("data-gs-x"));
        var posY = parseInt($(this).attr("data-gs-y"));

        // Rotate Ships Mechanics...
        if (w>h) {
            if ( grid.isAreaEmpty(posX, posY+1, h, w-1) && posX+h<=10 && posY+w<=10 ) {
                grid.update($(this), posX, posY, h, w);
            } else if ( grid.isAreaEmpty(posX, posY-w+1, h, w-1) && posX+h<=10 && posY-w+1>=0 ) {
                grid.update($(this), posX, posY-w+1, h, w);
            } else {
                searchSpaceAndRotate($(this));
            }
        } else if (h>w) {
            if ( grid.isAreaEmpty(posX+1, posY, h-1, w) && posX+h<=10 ) {
                grid.update($(this), posX, posY, h, w);
            } else if ( grid.isAreaEmpty(posX+1, posY+1, h-1, w) && posX+h<=10 ) {
                grid.update($(this), posX, posY+1, h, w);
            } else if ( grid.isAreaEmpty(posX+1, posY+2, h-1, w) && posX+h<=10 ) {
                grid.update($(this), posX, posY+2, h, w);
            } else if ( grid.isAreaEmpty(posX, posY-1, h, w) && posX+h<=10 && posY>0) {
                grid.update($(this), posX, posY-1, h, w);
            } else if ( grid.isAreaEmpty(posX, posY-2, h, w) && posX+h<=10 && posY>1) {
                grid.update($(this), posX, posY-2, h, w);
            } else {
                searchSpaceAndRotate($(this));
            }
        }
        // When no space near to rotate, search the first available in Grid...
        function searchSpaceAndRotate(widget) {
            for (var j=0; j<10; j++) {
                var found = false;
                for (var i=0; i<10; i++) {
                    if ( grid.isAreaEmpty(i, j, h, w) && i+h<=10 && j+w<=10 ) {
                        grid.update(widget, i, j, h, w);
                        found = true;
                        break;
                    }
                }
                if (found===true){break;}
            }
        }
        // Ship Img rotation...
        var shipImgId = $(this).children().attr("id");
        switch (shipImgId) {
            case "light-cruiser-img-v":
                $(this).children().attr("id", "light-cruiser-img-h").attr("src", "css/images/icons/light-cruiser-h.png");
                break;
            case "light-cruiser-img-h":
                $(this).children().attr("id", "light-cruiser-img-v").attr("src", "css/images/icons/light-cruiser-v.png");
                break;
            case "light-destroyer-img-h":
                $(this).children().attr("id", "light-destroyer-img-v").attr("src", "css/images/icons/light-destroyer-v.png");
                break;
            case "light-destroyer-img-v":
                $(this).children().attr("id", "light-destroyer-img-h").attr("src", "css/images/icons/light-destroyer-h.png");
                break;
            case "light-bomber-img-v":
                $(this).children().attr("id", "light-bomber-img-h").attr("src", "css/images/icons/light-bomber-h.png");
                break;
            case "light-bomber-img-h":
                $(this).children().attr("id", "light-bomber-img-v").attr("src", "css/images/icons/light-bomber-v.png");
                break;
            case "light-fighter-img-h":
                $(this).children().attr("id", "light-fighter-img-v").attr("src", "css/images/icons/light-fighter-v.png");
                break;
            case "light-fighter-img-v":
                $(this).children().attr("id", "light-fighter-img-h").attr("src", "css/images/icons/light-fighter-h.png");
                break;
            case "light-starfighter-img-v":
                $(this).children().attr("id", "light-starfighter-img-h").attr("src", "css/images/icons/light-starfighter-h.png");
                break;
            case "light-starfighter-img-h":
                $(this).children().attr("id", "light-starfighter-img-v").attr("src", "css/images/icons/light-starfighter-v.png");
                break;
            default:
                $(this).children().attr("id", "default-img").attr("src", "css/images/star.png");
        }
    })
}

//-------------------------------------------------------PLACE NEW SHIPS WITH GRIDSTACK FRAMEWORK---------------------------------------------------------
function placeNewShips() {
    $("#place-ships-card").show();

    var options = {
        //grilla de 10 x 10
        width: 10,
        height: 10,
        //separacion entre elementos (les llaman widgets)
        verticalMargin: 0,
        //altura de las celdas
        cellHeight: 35,
        cellWidth: 35,
        //desabilitando el resize de los widgets
        disableResize: true,
        //widgets flotantes
        float: true,
        //removeTimeout: 100,
        //permite que el widget ocupe mas de una columna
        disableOneColumnMode: true,
        //false permite mover, true impide
        staticGrid: false,
        //activa animaciones (cuando se suelta el elemento se ve más suave la caida)
        animate: true
    }
    //se inicializa el grid con las opciones
    $('.grid-stack').gridstack(options);
    var grid = $('#grid').data('gridstack');

    grid.addWidget($('<div><img id="light-cruiser-img-v" class="grid-stack-item-content" src="css/images/icons/light-cruiser-v.png" alt="cruiser"></div>'),
    1, 0, 1, 5, false);
    grid.addWidget($('<div><img id="light-destroyer-img-v" class="grid-stack-item-content" src="css/images/icons/light-destroyer-v.png" alt="destroyer"></div>'),
    8, 2, 1, 4, false);
    grid.addWidget($('<div><img id="light-bomber-img-v" class="grid-stack-item-content" src="css/images/icons/light-bomber-v.png" alt="bomber"></div>'),
    4, 2, 1, 3, false);
    grid.addWidget($('<div><img id="light-fighter-img-h" class="grid-stack-item-content" src="css/images/icons/light-fighter-h.png" alt="fighter"></div>'),
    1, 7, 3, 1, false);
    grid.addWidget($('<div><img id="light-starfighter-img-v" class="grid-stack-item-content" src="css/images/icons/light-starfighter-v.png" alt="starFighter"></div>'),
    6, 7, 1, 2, false);

    setListener(grid);
}


//-------------------------------------------------------LOAD SHIPS FROM BACKEND WITH GRIDSTACK FRAMEWORK---------------------------------------------------------
function placeShipsFromBackEnd() {
    $("#place-ships-card").hide();

    var options = {
        width: 10,
        height: 10,
        verticalMargin: 0,
        cellHeight: 35,
        cellWidth: 35,
        disableResize: true,
        float: true,
        disableOneColumnMode: true,
        staticGrid: true,
        animate: true
    }
    $('.grid-stack').gridstack(options);
    var grid = $('#grid').data('gridstack');

    app.gameViewerShips.map(function(ship) {
        var searchChar = ship.locations[0].slice(0, 1);
        var secondChar = ship.locations[1].slice(0, 1);
        if ( searchChar === secondChar ) {
            //si es la misma letra es porque es horizontal
            ship.position = "Horizontal";
        } else {
            ship.position = "Vertical";
        }
        //Que hace acá?
        for (var i=0; i < ship.locations.length; i++) {
            ship.locations[i] = ship.locations[i].replace(/A/g, '0');
            ship.locations[i] = ship.locations[i].replace(/B/g, '1');
            ship.locations[i] = ship.locations[i].replace(/C/g, '2');
            ship.locations[i] = ship.locations[i].replace(/D/g, '3');
            ship.locations[i] = ship.locations[i].replace(/E/g, '4');
            ship.locations[i] = ship.locations[i].replace(/F/g, '5');
            ship.locations[i] = ship.locations[i].replace(/G/g, '6');
            ship.locations[i] = ship.locations[i].replace(/H/g, '7');
            ship.locations[i] = ship.locations[i].replace(/I/g, '8');
            ship.locations[i] = ship.locations[i].replace(/J/g, '9');
        }

        var yInGrid = parseInt(ship.locations[0].slice(0, 1));
        var xInGrid = parseInt(ship.locations[0].slice(1, 3)) - 1;

        if (ship.type.toLowerCase() === "cruiser") {
            if (ship.position === "Horizontal") {
                grid.addWidget($('<div><img id="light-cruiser-img-h" class="grid-stack-item-content" src="css/images/icons/light-cruiser-h.png" alt="cruiser"></div>'),
                xInGrid, yInGrid, 5, 1, false);
            } else if (ship.position === "Vertical") {
                grid.addWidget($('<div><img id="light-cruiser-img-v" class="grid-stack-item-content" src="css/images/icons/light-cruiser-v.png" alt="cruiser"></div>'),
                xInGrid, yInGrid, 1, 5, false);
            }
        } else if (ship.type.toLowerCase() === "destroyer") {
            if (ship.position === "Horizontal") {
                grid.addWidget($('<div><img id="light-destroyer-img-h" class="grid-stack-item-content" src="css/images/icons/light-destroyer-h.png" alt="destroyer"></div>'),
                xInGrid, yInGrid, 4, 1, false);
            } else if (ship.position === "Vertical") {
                grid.addWidget($('<div><img id="light-destroyer-img-v" class="grid-stack-item-content" src="css/images/icons/light-destroyer-v.png" alt="destroyer"></div>'),
                xInGrid, yInGrid, 1, 4, false);
            }
        } else if (ship.type.toLowerCase() === "bomber") {
            if (ship.position === "Horizontal") {
                grid.addWidget($('<div><img id="light-bomber-img-h" class="grid-stack-item-content" src="css/images/icons/light-bomber-h.png" alt="bomber"></div>'),
                xInGrid, yInGrid, 3, 1, false);
            } else if (ship.position === "Vertical") {
                grid.addWidget($('<div><img id="light-bomber-img-v" class="grid-stack-item-content" src="css/images/icons/light-bomber-v.png" alt="bomber"></div>'),
                xInGrid, yInGrid, 1, 3, false);
            }
        } else if (ship.type.toLowerCase() === "fighter") {
            if (ship.position === "Horizontal") {
                grid.addWidget($('<div><img id="light-fighter-img-h" class="grid-stack-item-content" src="css/images/icons/light-fighter-h.png" alt="fighter"></div>'),
                xInGrid, yInGrid, 3, 1, false);
            } else if (ship.position === "Vertical") {
                grid.addWidget($('<div><img id="light-fighter-img-v" class="grid-stack-item-content" src="css/images/icons/light-fighter-v.png" alt="fighter"></div>'),
                xInGrid, yInGrid, 1, 3, false);
            }

        } else if (ship.type.toLowerCase() === "starfighter") {
              if (ship.position === "Horizontal") {
                  grid.addWidget($('<div><img id="light-starfighter-img-h" class="grid-stack-item-content" src="css/images/icons/light-starfighter-h.png" alt="starFighter"></div>'),
                  xInGrid, yInGrid, 2, 1, false);
              } else if (ship.position === "Vertical") {
                  grid.addWidget($('<div><img id="light-starfighter-img-v" class="grid-stack-item-content" src="css/images/icons/light-starfighter-v.png" alt="starFighter"></div>'),
                  xInGrid, yInGrid, 1, 2, false);
              }
        }
    })
}

//-------------------------------------------------------GET CURRENT TURN---------------------------------------------------------
function getCurrentTurn(arrayOfSalvos) {
    $("#fire-card").removeClass("display-none").addClass("display-block");
    var allTurnsFromViewer = [];
    var allTurnsFromOpponent = [];
    arrayOfSalvos.map(function(salvo) {
        if (salvo.player === app.viewerPlayerId) {
            allTurnsFromViewer.push(parseInt(salvo.turn));
        }else{
            allTurnsFromOpponent.push(parseInt(salvo.turn));
        }
    })
    var currentTurn = function(){if(allTurnsFromViewer.length===0){
                                    return 1;
                                 }else{
                                    return Math.max(...allTurnsFromViewer)+1;
                                 }
                      };
    $("#turn-number").html(currentTurn);
    app.viewerSalvoTurn=app.viewerGameState;
}




//----------------------------------------------------------------AJAX POST SALVOS-----------------------------------------------------------------
function postSalvos(salvoJSON) {
    var gamePlayerId = getQueryVariable("gp");
    console.log("Mi salvoJSON: ",salvoJSON);
    $.post({
        url: "/api/games/players/"+gamePlayerId+"/salvos",
        data: JSON.stringify(salvoJSON),
        dataType: "text",
        contentType: "application/json"
    })
    .done(function (response) {
        if ($("#salvo-action").hasClass("game-play-alert")) {
            $("#salvo-action").removeClass("game-play-alert");
        }
        refreshData();
        loadData();
        console.log( "Salvo added: " + response );

    })
    .fail(function (jqXHR, textStatus, error) {
        console.log("Failed to add salvo... " + jqXHR.responseText);
        app.viewerSalvoTurn=jqXHR.responseText;
    })
}

//-------------------------------------------------------WHEN SHIPS CREATED...---------------------------------------------------------

    //-------------------------------------------------------CREATE SALVOS IN GRID---------------------------------------------------------
    $("#salvo-body > tr > td").click(function() {
        if ( $(this).hasClass("bg-salvo") ) {
            return;
        } else if ( $(this).children().length > 0 ) {
            $(this).html("");
        } else if ( $(".aim-img").length < 5 ) {
            var letter = $(this).parent().attr("class");
            var number = $(this).attr("class");
            var cell = letter+number;

            $(this).html("<img data-cell='"+cell+"'class='aim-img' src='css/images/aim.png'>");
        }
    })
    //-------------------------------------------------ON CLICK FIRE - POST NEW SALVO------------------------------------------------------
    $("#salvo-col").on("click", "#fire-salvo-btn", function(){

        if ( $(".aim-img").length != 0 && $(".aim-img").length <= 5 ) {
            playFireSound();
            var salvoJSON = {};
            var turn = $("#turn-number").text();
            var locations = [];
            $(".aim-img").each(function() {
               locations.push($(this).data("cell"));
            })
            salvoJSON.turn = turn;
            salvoJSON.locations = locations;
            postSalvos(salvoJSON);

        } else {
            if (!$("#salvo-action").hasClass("game-play-alert")) {
                $("#salvo-action").addClass("game-play-alert");
            }
        }
    })


//-------------------------------------------------------DISPLAY SALVOS FUNCTION---------------------------------------------------------
function displaySalvoes(gamePlayerId, gameDTO) {
    console.log("Ingrese a display salvoes");
   for (var i=0;i<gameDTO.gamePlayers.length;i++){

       if (gameDTO.gamePlayers[i].gpid == gamePlayerId) {
           var thisPlayerId = gameDTO.gamePlayers[i].id;
           gameDTO.salvoes.map(function (salvo) {

               if (salvo.player == thisPlayerId) {
               // --------------------------------------------------- HITS or MISSES ----------------------------------------------------
                console.log("los salvos de este jugador:",salvo.locations);
                   //var myTurn = salvo.turn;
                   for (var e=0;e<salvo.locations.length;e++){
                       var letterP1 = salvo.locations[e].substring(0, 1);
                       var numberP1 = salvo.locations[e].substring(1, 3);

                       if (salvo.hits.indexOf(salvo.locations[e]) != -1) {
                            $("#salvo-body>."+letterP1+" td:eq("+numberP1+")").html('<img class="spark-salvo" src="css/images/spark.png">');
                       } else {
                            $("#salvo-body>."+letterP1+" td:eq("+numberP1+")").html('<img class="spark-salvo" src="css/images/cross.png">');
                       }
                   }
                   // --------------------------------------------------- SINKS ----------------------------------------------------
                   for (var ss=0;ss<salvo.sinks.length;ss++) {

                        for (var s=0;s<salvo.sinks[ss].locations.length;s++) {

                             var sinkLetter = salvo.sinks[ss].locations[s].substring(0, 1);
                             var sinkNumber = salvo.sinks[ss].locations[s].substring(1, 3);
                             var sinkCell = $("#salvo-body>."+sinkLetter+" td:eq("+sinkNumber+")");

                             if (!sinkCell.hasClass("bg-salvo")) {
                                 sinkCell.addClass("bg-salvo");
                             }
                        }

                        switch (salvo.sinks[ss].type) {
                            case "cruiser":
                                $("#light-cruiser-img-v2").attr("src", "css/images/icons/light-cruiser-v2.png");
                                break;
                            case "destroyer":
                                $("#light-destroyer-img-v2").attr("src", "css/images/icons/light-destroyer-v2.png");
                                break;
                            case "bomber":
                                $("#light-bomber-img-v2").attr("src", "css/images/icons/light-bomber-v2.png");
                                break;
                            case "fighter":
                                $("#light-fighter-img-h2").attr("src", "css/images/icons/light-fighter-h2.png");
                                break;
                            case "starFighter":
                                $("#light-starfighter-img-v2").attr("src", "css/images/icons/light-starfighter-v2.png");
                                break;
                            default:
                                break;
                            }
                   }
               // --------------------------------------------------- MY GRID ----------------------------------------------------
               } else if (salvo.player != thisPlayerId) {
                 // --------------------------------------------------- HITS or MISSES -------------------------------------------
                   for (var h=0;h<salvo.locations.length;h++){
                       var letter = salvo.locations[h].substring(0, 1);
                       var number = salvo.locations[h].substring(1, 3)-1;

                       switch(letter) {
                            case "A":letter = 0;break;
                            case "B":letter = 1;break;
                            case "C":letter = 2;break;
                            case "D":letter = 3;break;
                            case "E":letter = 4;break;
                            case "F":letter = 5;break;
                            case "G":letter = 6;break;
                            case "H":letter = 7;break;
                            case "I":letter = 8;break;
                            case "J":letter = 9;break;
                            default:letter = 0;break;
                       }

                       if ( app.allShipPositions.indexOf(salvo.locations[h]) != -1 ) {
                           $('#grid').append('<div style="position:absolute; top:'+letter*35+'px; left:'+number*35+'px;"><img class="spark" src="css/images/spark.png"></div>');
                       } else {
                           $('#grid').append('<div style="position:absolute; top:'+letter*35+'px; left:'+number*35+'px;"><img class="spark" src="css/images/cross.png"></div>');
                       }

                   }
               }
           });
       }
   }
}

//----------------------------------------------WHEN SHIPS CREATED...----------------------------------------------
function playFireSound() {
   var fireAudio = document.getElementById("fire-audio");
   fireAudio.play();
}

//----------------------------------------------------GET ALL SHIP CELLS LOCATION TO COMPARE WITH SALVOS---------------------------------------------------------
function getAllShipLocations(set) {
    set.map(function(ship) {
        for (var i=0; i<ship.locations.length; i++){
            app.allShipPositions.push(ship.locations[i]);
        }
    });
    console.log(app.allShipPositions);
    return;
}

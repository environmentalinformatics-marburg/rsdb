//from https://stackoverflow.com/questions/24050738/javascript-how-to-dynamically-move-div-by-clicking-and-dragging
function drag_div(div_id){
    console.log("create");
    var div = document.getElementById(div_id);
    
    div.addEventListener('mousedown', function(e) {
        console.log("mousedown");
        div.isDown = true;
        div.offset = [
            div.offsetLeft - e.clientX,
            div.offsetTop - e.clientY
        ];
    }, true);
    console.log("added");
    
    div.addEventListener('mouseup', function() {
        div.isDown = false;
    }, true);
    
    div.addEventListener('mousemove', function(event) {
        event.preventDefault();
        if (div.isDown) {
            div.mousePosition = {    
                x : event.clientX,
                y : event.clientY,    
            };
            div.style.left = (div.mousePosition.x + div.offset[0]) + 'px';
            div.style.top  = (div.mousePosition.y + div.offset[1]) + 'px';
        }
    }, true);
}
$(document).ready(function(){
    $('#add-ct-var-modal #add-ct-var-type .btn').on('click', function(){
        $('#add-ct-var-modal #add-ct-var-type .btn').removeClass('active');
        $(this).addClass('active');
    });
    $('#add-ct-var-modal #add-ct-var-btn').on('click', function(){
        var name = $('#add-ct-var-modal #add-ct-var-name').val(),
        type = $('#add-ct-var-modal #add-ct-var-type .btn.active').attr('value');
    if(type == "number" || type == "string"){
        $('#human_text').val($('#human_text').val()+"<["+name+":"+type+"]>");
    }
    });

    $('#refer-ct-var-btn').on('click', function(){
        alert('here');
        var text = $('#human_text').val(),
        rex = /<\[([a-zA-Z0-9]+):([a-zA-Z0-9]+)\]>/,
        rex2 = /<\[([a-zA-Z0-9]+):([a-zA-Z0-9]+)\]>/g,
        matches = text.match(rex2),
        i;
    for(i = 0; i < matches.length; i++){
        var match = matches[i],
            varname = match.match(rex)[1];
    }
    });
});

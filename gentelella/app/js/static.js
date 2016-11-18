/**
 * Created by dbourasseau on 14/11/16.
 */
var values = ["1 Mbps", "2 Mbps", "3 Mbps", "4 Mbps", "5 Mbps", "6 Mbps", "7 Mbps", "8 Mbps", "9 Mbps", "10 Mbps", "20 Mbps", "30 Mbps", "40 Mbps", "50 Mbps", "60 Mbps", "70 Mbps", "80 Mbps", "90 Mbps", "100 Mbps", "200 Mbps", "300 Mbps", "400 Mbps", "500 Mbps", "600 Mbps", "700 Mbps", "800 Mbps", "900 Mbps", "1 Gbps", "2 Gbps", "3 Gbps", "4 Gbps", "5 Gbps", "6 Gbps", "7 Gbps", "8 Gbps", "9 Gbps", "10 Gbps", "20 Gbps", "30 Gbps", "40 Gbps", "50 Gbps", "60 Gbps", "70 Gbps", "80 Gbps", "90 Gbps", "100 Gbps", "200 Gbps", "300 Gbps", "400 Gbps", "500 Gbps", "600 Gbps", "700 Gbps", "800 Gbps", "900 Gbps", "1 Tbps"];


$(window).scroll(function () {

    if ($("#videoHDWindow").is(":visible")) {
        valuelow = $("#videoHDWindow").position().top
    }
    else {
        valuelow = $("#publi").position().top;
    }
    console.log(valuelow);
    lawerScroll = $(document).height() // - $(window).height()
    if ($(window).scrollTop() > ($('#general_pres').position().top + $('#general_pres').height()) && ($(window).scrollTop() + $('#move').height()) <= (valuelow )) {
        $("#move").stop().animate({

            "marginTop": ($(window).scrollTop()) + "px",
            // "marginLeft": ($(window).scrollLeft()) + "px"
        }, 0);
    }
    else if ($(window).scrollTop() < ($('#general_pres').position().top + $('#general_pres').height() )) {
        $("#move").stop().animate({

            "marginTop": ($('#general_pres').position().top + $('#general_pres').height() ) + "px",
            // "marginLeft": ($(window).scrollLeft()) + "px"
        }, 0);
    }
    else if (($(window).scrollTop() + $('#move').height()) > (valuelow )) {
        $("#move").stop().animate({

            "marginTop": (valuelow - $('#move').height()) + "px",
            // "marginLeft": ($(window).scrollLeft()) + "px"
        }, 0);
    }

});

$(window).resize(function () {
    $("#movesize").height($('#move').height());
});

$(document).ready(function () {
    $("#move").stop().animate({

        "marginTop": ($('#general_pres').position().top + $('#general_pres').height() ) + "px",
        // "marginLeft": ($(window).scrollLeft()) + "px"
    }, 0);
    $("#move").show();
    $("#movesize").height($('#move').height());

});

$("#edge-bw").ionRangeSlider(
    {
        from: 36,
        values: values,
        keyboard: true
    });
sliderNbUser = $("#edge-delay").ionRangeSlider({
    min: 0,
    max: 15,
    from: 1,
    step: 0.1,
    keyboard: true
});

sliderNbUser = $("#node-cpu").ionRangeSlider({
    min: 1,
    max: 2000,
    from: 200,
    keyboard: true
});

function clearPopUp() {
    document.getElementById('saveButton').onclick = null;
    document.getElementById('cancelButton').onclick = null;
    $('#node-modal').modal("hide");

    document.getElementById('saveEdgeButton').onclick = null;
    document.getElementById('cancelEdgeButton').onclick = null;
    $('#edge-modal').modal("hide");
}

function cancelEdit(callback) {
    clearPopUp();
    callback(null);
}

function saveNodeData(data, callback) {
    data.id = document.getElementById('node-id').value;
    data.label = data.id;
    data.cpu = document.getElementById('node-cpu').value;
    data.title = 'cpu:' + data.cpu;
    data.value = data.cpu;
    data.shape = neutralNode.shape;
    data.style = neutralNode.style;
    data.color = neutralNode.color;
    data.width = neutralNode.width;
    data.font = neutralNode.font;
    data.shadow = {};
    clearPopUp();
    callback(data);
}

function saveEdgeData(data, callback) {
    data.id = document.getElementById('edge-id').value;
    // data.label = data.id;

    var bwrow = $("#edge-bw").data("ionRangeSlider").result.from_value;

    data.bw = humanFormat.parse(bwrow, {unit: 'bps'});

    data.delay = document.getElementById('edge-delay').value;
    data.title = 'id:"' + data.id + '" bw:' + humanFormat(parseInt(data.bw), {
            unit: 'b'
        }) + '/s delay: ' + data.delay;
    data.penwidth = neutralEdge.penwidth;
    data.font = neutralEdge.font;
    data.len = neutralEdge.len;
    data.color = neutralEdge.color;

    data.value = Math.log(data.bw);

    clearPopUp();
    callback(data);
}

function init() {
    setDefaultLocale();
    draw();
}

function destroy() {
    if (network !== null) {
        network.destroy();
        network = null;
    }
}

function networkload() {
    data = {
        nodes: nodes,
        edges: edges
    };

    options = {
        nodes: {
            shape: 'dot',
            size: 16,

            scaling: {
                label: {
                    min: 8,
                    max: 20
                }
            },
        },
        layout: {
            randomSeed: 34
        },
        interaction: {hover: true},


        manipulation: {
            addNode: function (data, callback) {

                $('#Operation').innerHTML = "Create Node";
                document.getElementById('node-id').value = data.id;
                document.getElementById('node-cpu').value = 200;
                document.getElementById('saveButton').onclick = saveNodeData.bind(this, data, callback);
                document.getElementById('cancelButton').onclick = clearPopUp.bind();
                $('#node-modal').modal("show");

            },
            editNode: function (data, callback) {
                // filling in the popup DOM elements
                $('#Operation').innerHTML = "Edit Node";
                document.getElementById('node-id').value = data.id;
                // document.getElementById('node-cpu').value = data.cpu;
                $("#node-cpu").data('ionRangeSlider').update({from: data.cpu});
                document.getElementById('saveButton').onclick = saveNodeData.bind(this, data, callback);
                document.getElementById('cancelButton').onclick = cancelEdit.bind(this, callback);
                $('#node-modal').modal("show");


            },
            addEdge: function (data, callback) {
                if (data.from != data.to) {
                    $('#EdgeOperation').innerHTML = "Add edge";
                    document.getElementById('edge-id').value = data.from + "--" + data.to;
                    document.getElementById('edge-bw').value = 1000000000;
                    document.getElementById('edge-delay').value = 3;
                    document.getElementById('saveEdgeButton').onclick = saveEdgeData.bind(this, data, callback);
                    document.getElementById('cancelEdgeButton').onclick = cancelEdit.bind(this, callback);
                    $('#edge-modal').modal("show");
                }
            },


            // editEdge: false
            editEdge: function (data, callback) {
                if (data.from != data.to) {
                    $('#EdgeOperation').innerHTML = "Add edge";
                    edge = edges.get(data.id)
                    document.getElementById('edge-id').value = data.id;
                    // document.getElementById('edge-bw').value = edge.bw;
                    bwstr = humanFormat(edge.bw, {unit: 'bps'});
                    bwpos = values.indexOf(bwstr)
                    $("#edge-bw").data('ionRangeSlider').update({from: bwpos});
                    $("#edge-delay").data('ionRangeSlider').update({from: edge.delay});
                    document.getElementById('saveEdgeButton').onclick = saveEdgeData.bind(this, data, callback);
                    document.getElementById('cancelEdgeButton').onclick = cancelEdit.bind(this, callback);
                    $('#edge-modal').modal("show");
                    // callback(data);
                }
            }
        }
    };

    container = document.getElementById('mynetwork');
    network = new vis.Network(container, data, options);


}
/** @jsx React.DOM */

var $ = require('jquery');
var React = require('react');
var Grid = require('react-bootstrap/Grid');
var Row = require('react-bootstrap/Row');
var Col = require('react-bootstrap/Col');
var Panel = require('react-bootstrap/Panel');
var Input = require('react-bootstrap/Input');
var Accordion = require('react-bootstrap/Accordion');

var d3 = require('d3');
var dagreD3 = require('dagre-d3');

var TxExplorerForm = React.createClass({
    getInitialState: function(){
        return {txHash:''};
    },
    handleChange: function(){
        this.setState({txHash: this.refs.txHash.getValue()});
    },
    handleSubmit: function(e){
        e.preventDefault();

        var p = this.props,
            s = $.extend({}, this.state);
        s.role = p.role;
        p.handleSubmit(s);
    },
    render: function(){
        return (
            <form onSubmit={this.handleSubmit}>
                <Input type="text" value={this.state.txHash} placeholder="Enter a transaction hash"
                    label="TX Hash" ref="txHash" onChange={this.handleChange} />
                <Input type="submit" bsStyle='primary' value="Explore" />
            </form>
        );
    }
});

var ExplorerSidebar = React.createClass({
    render: function(){
        return (
            <Panel header="Explore Transactions">
                <Accordion>
                    <Panel header="Show immediate txs" key={1}>
                        <TxExplorerForm role="show_immediate_txs" handleSubmit={this.props.handleSubmit} />
                    </Panel>
                    <Panel header="Show all txs until coinbase" key={2}>
                        <TxExplorerForm role="until_coinbase" handleSubmit={this.props.handleSubmit} />
                    </Panel>
                </Accordion>
            </Panel>
        );
    }
});

var VizImmediateTxs = React.createClass({
    componentDidMount: function(){
        var that = this;
        $.get("/graph/tx/get-inputs/"+this.props.dm.txHash, function(data){
            if(data.nodes == null){ return; }
            that.draw(data);
        });
    },
    draw: function(data){
        var g = new dagreD3.Digraph(),
            props = this.props.dm;
        g.addNode(props.txHash, { label: props.txHash});
        for(var i = 0; i < data.nodes.length; i++){
            var node = data.nodes[i];
            g.addNode(node, { label: node });
        }

        for(var i = 0; i < data.relations.length; i++){
            var relation = data.relations[i];
            g.addEdge(null, relation.hash, props.txHash, { label: relation.value.toString() });
        }

        var renderer = new dagreD3.Renderer();
        var layout = dagreD3.layout().rankDir("LR");
        renderer.layout(layout).run(g, d3.select(this.refs.mainviz.getDOMNode()));
    },
    render: function(){
        var dm = this.props.dm;
        return (
            <Panel header={"Showing intermediate txs for "+dm.txHash} bsStyle="primary">
                <svg width="100%" height={680}>
                    <g ref="mainviz" transform="translate(20,20)"/>
                </svg>
            </Panel>
        );
    }
});

var VizUntilCoinbase = React.createClass({
    componentDidMount: function(){
        var that = this;
        $.get("/graph/tx/get-until-coinbase/"+this.props.dm.txHash, function(data){
            if(data.nodes == null){ return; }
            that.draw(data);
        });
    },
    draw: function(data){
        var g = new dagreD3.Digraph(),
            props = this.props.dm;
        g.addNode(props.txHash, { label: props.txHash});
        for(var i = 0; i < data.nodes.length; i++){
            var node = data.nodes[i];
            g.addNode(node, { label: node });
        }

        for(var i = 0; i < data.relations.length; i++){
            var relation = data.relations[i];
            g.addEdge(null, relation.in.hash, relation.out.hash, { label: relation.details.value.toString() });
        }

        var renderer = new dagreD3.Renderer();
        var layout = dagreD3.layout().rankDir("LR");
        renderer.layout(layout).run(g, d3.select(this.refs.mainviz.getDOMNode()));
    },
    render: function(){
        var dm = this.props.dm;
        return (
            <Panel header={"Showing txs until coinbase for "+dm.txHash} bsStyle="primary">
                <svg width="100%" height={680}>
                    <g ref="mainviz" transform="translate(20,20)"/>
                </svg>
            </Panel>
            );
    }
});

var ExplorerVisualization = React.createClass({
    getInitialState: function(){
        return {viz: <div>No visualizations</div>};
    },
    visualize: function(datamap){
        switch (datamap.role){
            case "show_immediate_txs":
                this.setState({viz: <VizImmediateTxs dm={datamap} />});
                break;

            case "until_coinbase":
                this.setState({viz: <VizUntilCoinbase dm={datamap} />});
                break;
        }
    },
    render: function(){
        return this.state.viz;
    }
});

var Explorer = React.createClass({
    handleSubmit: function(datamap){
        this.refs.viz.visualize(datamap);
    },
    render: function(){
        return (
            <Grid fluid={true}>
                <Row>
                    <Col xs={4}>
                        <ExplorerSidebar handleSubmit={this.handleSubmit} />
                    </Col>
                    <Col xs={8}>
                        <ExplorerVisualization ref="viz"/>
                    </Col>
                </Row>
            </Grid>
        );
    }
});

module.exports = Explorer;
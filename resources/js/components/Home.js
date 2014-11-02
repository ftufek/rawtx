/** @jsx React.DOM */

var React = require('react');
var Grid = require('react-bootstrap/Grid');
var Row = require('react-bootstrap/Row');
var Col = require('react-bootstrap/Col');

var Home = React.createClass({
    render: function(){
        return (
            <Grid fluid={true}>
                <Row>
                    <Col xs={12} className="text-center">
                        <h2>Welcome to rawTX, a graphical exploration tool for the bitcoin blockchain!</h2>
                        <img src="/img/graph.png" alt="Graph image"/>
                    </Col>
                </Row>
            </Grid>
        );
    }
});

module.exports = Home;
/** @jsx React.DOM */

var $ = require('jquery');

var React = require('react');
var Grid = require('react-bootstrap/Grid');
var Row = require('react-bootstrap/Row');
var Col = require('react-bootstrap/Col');
var Table = require('react-bootstrap/Table');

var Information = React.createClass({
    getInitialState: function(){
        return {
            'num-blk-indexed': 'Loading...',
            'num-tx-indexed': 'Loading...'
        };
    },
    componentWillMount: function(){
        var that = this;
        $.get('/graph/stats', function(data){
            that.setState(data);
        });
    },
    render: function(){
        return (
            <Grid>
                <Row>
                    <Col xs={12}>
                        <Table striped bordered condensed hover>
                            <thead>
                                <tr>
                                    <th>Statistic</th>
                                    <th>Value</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>Number of blocks indexed</td>
                                    <td>{this.state['num-blk-indexed']}</td>
                                </tr>
                                <tr>
                                    <td>Number of transactions indexed</td>
                                    <td>{this.state['num-tx-indexed']}</td>
                                </tr>
                            </tbody>
                        </Table>
                    </Col>
                </Row>
            </Grid>
        );
    }
});

module.exports = Information;
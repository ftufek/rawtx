/** @jsx React.DOM */

var React = require('react');
var Navbar = require('react-bootstrap/Navbar');
var Nav = require('react-bootstrap/Nav');
var NavItem = require('react-bootstrap/NavItem');

var ReactRouter = require('react-router');
var Link = ReactRouter.Link;

var Navigation = React.createClass({
    render: function(){
        return (
            <Navbar brand="rawTX">
                <Nav>
                    <li><Link to="app">Home</Link></li>
                    <li><Link to="explore">Explore</Link></li>
                    <li><Link to="information">Information</Link></li>
                </Nav>
            </Navbar>
        );
    }
});

module.exports = Navigation;
/**
 * Created with IntelliJ IDEA.
 * User: allan
 * Date: 8/9/12
 * Time: 17:05
 * To change this template use File | Settings | File Templates.
 */

var theRealmList =

Ext.define('AM.view.GlobalEditor', {
    extend: 'Ext.panel.Panel',
    alias:   'widget.globaleditor',

    title:   'Services',
    layout: 'vbox',
    items: [
        {
            xtype:'servicelist',
            width:'100%',
            margin: '5 5 5 5',
            autoScroll: true,
            store: 'RealmStore',
            flex:1
        },
        {
            xtype: 'panel',
            layout: 'column',
            width: '100%',
            flex: 8,
            autoScroll: true,

            items: [
                {
                    xtype:'servicelist',
                    width:'20%',
                    margin: '5 5 5 5',
                    autoScroll: true,
                    store: 'ServiceStore',
                    flex:2
                },
                {
                    xtype: 'serviceeditor',
                    flex: 1,
                    margin: '5 5 5 5',
                    height: 400,
                    width: '78%'
                }
            ]
        }
    ],


    initComponent: function() {
        console.log('Initializing GlobalEditor height ');
        this.callParent(arguments);
    }
});
